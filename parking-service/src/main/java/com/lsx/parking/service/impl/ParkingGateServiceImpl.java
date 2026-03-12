package com.lsx.parking.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.core.common.Util.RedisLockUtil;
import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.dto.ParkingGateOpenDTO;
import com.lsx.parking.entity.*;
import com.lsx.parking.mapper.*;
import com.lsx.parking.service.ParkingGateService;
import com.lsx.parking.vo.ParkingGateExitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingGateServiceImpl implements ParkingGateService {

    private final ParkingSpaceMapper parkingSpaceMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final ParkingGateLogMapper gateLogMapper;
    private final ParkingSpaceLeaseMapper leaseMapper;
    private final VehicleMapper vehicleMapper;
    private final RedisLockUtil redisLockUtil;

    @Override
    @Transactional
    public void enterGate(ParkingGateEnterDTO dto) {

        if (!StringUtils.hasText(dto.getPlateNo())) {
            throw new RuntimeException("车牌号不能为空");
        }

        String plateNo = dto.getPlateNo();  // 这里获取了车牌号

        // 1. 根据车牌查车辆
        Vehicle vehicle = vehicleMapper.selectByPlateNo(plateNo);

        boolean isOwnerVehicle = vehicle != null;
        ParkingSpaceLease lease = null;

        if (isOwnerVehicle) {
            // 2. 查是否有有效车位使用权（包月 / 固定）
            lease = leaseMapper.selectActiveLeaseByUser(vehicle.getUserId());
        }

        // 3. 记录入闸日志
        ParkingGateLog log = new ParkingGateLog();
        log.setUserId(isOwnerVehicle ? vehicle.getUserId() : null);
        log.setSpaceId(lease != null ? lease.getSpaceId() : null);
        log.setGateType(isOwnerVehicle ? "OWNER" : "TEMP");
        log.setAction("ENTER");
        log.setResult("SUCCESS");

        // ⭐⭐ 新增：设置车牌号 ⭐⭐
        log.setPlateNo(plateNo);  // 这里必须设置！
        if (!isOwnerVehicle) {
            log.setRemark("外来车辆入闸");
        } else if (lease != null) {
            log.setRemark("业主固定车位入闸");
        } else {
            log.setRemark("业主临时停车入闸");
        }

        log.setCreateTime(LocalDateTime.now());
        gateLogMapper.insert(log);

        // 4. 是否放行？
        // 说明：
        // - 固定车位 / 包月：直接放行
        // - 业主临停 / 外来车辆：也允许入闸，出闸时算钱
    }

    @Override
    @Transactional
    public ParkingGateExitResult exitGate(ParkingGateExitDTO dto) {
        String plateNo = dto.getPlateNo();

        // ==================【0️⃣：Redis 分布式锁配置】=================
        // Key：同一辆车（同一车牌）在同一时刻只能有一个出闸线程
        String lockKey = "lock:parking:exit:" + plateNo;
        // Value：用于解锁时校验，防止误删别人的锁
        String lockValue = UUID.randomUUID().toString();

        // ==================【1️⃣：带重试的分布式锁获取】=================
        // 配置参数：锁10秒过期，最多重试3次，每次等待100毫秒
        boolean locked = false;
        try {
            locked = redisLockUtil.tryLockWithRetry(lockKey, lockValue, 10, 3, 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【出闸中断】车牌 {} 获取锁被中断", plateNo, e);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        if (!locked) {
            // 🚫 多次重试后仍无法获取锁，说明该车辆正在被其他请求处理
            log.warn("【出闸冲突】车牌 {} 多次尝试获取锁失败，可能被恶意重复请求", plateNo);
            throw new RuntimeException("车辆正在出闸处理中，请稍后重试");
        }

        try {
            log.info("【出闸开始】车牌 {} 成功获取分布式锁，开始处理出闸逻辑", plateNo);

            // ==================【2️⃣：基础参数校验】=================
            if (!StringUtils.hasText(plateNo)) {
                throw new RuntimeException("车牌号不能为空");
            }

            LocalDateTime exitTime = LocalDateTime.now();

            // ==================【3️⃣：查询最近一次入闸记录】=================
            ParkingGateLog enterLog = gateLogMapper.selectOne(
                    Wrappers.<ParkingGateLog>lambdaQuery()
                            .eq(ParkingGateLog::getPlateNo, plateNo)
                            .eq(ParkingGateLog::getAction, "ENTER")
                            .orderByDesc(ParkingGateLog::getCreateTime)
                            .last("LIMIT 1")
            );

            if (enterLog == null) {
                log.warn("【出闸失败】车牌 {} 未找到入闸记录", plateNo);
                throw new RuntimeException("未找到入闸记录，无法出闸");
            }

            LocalDateTime enterTime = enterLog.getCreateTime();

            // ==================【4️⃣：固定车位校验】=================
            ParkingSpaceLease lease = null;
            if (enterLog.getUserId() != null) {
                lease = leaseMapper.selectOne(
                        Wrappers.<ParkingSpaceLease>lambdaQuery()
                                .eq(ParkingSpaceLease::getUserId, enterLog.getUserId())
                                .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                                .last("LIMIT 1")
                );
            }

            boolean isFixedOwner = lease != null &&
                    (lease.getEndTime() == null || lease.getEndTime().isAfter(exitTime));

            // ==================【5️⃣：固定车位 -> 直接放行】=================
            if (isFixedOwner) {
                log.info("【固定车位放行】车牌 {} 为固定车位业主，直接放行", plateNo);

                ParkingGateLog exitLog = new ParkingGateLog();
                exitLog.setPlateNo(plateNo);
                exitLog.setUserId(enterLog.getUserId());
                exitLog.setSpaceId(lease.getSpaceId());
                exitLog.setGateType("FIXED");
                exitLog.setAction("EXIT");
                exitLog.setResult("SUCCESS");
                exitLog.setRemark("固定车位业主直接放行");
                exitLog.setCreateTime(exitTime);
                gateLogMapper.insert(exitLog);

                ParkingGateExitResult result = new ParkingGateExitResult();
                result.setAllowPass(true);
                result.setNeedPay(false);
                result.setMessage("固定车位业主，直接放行");
                return result;
            }

            // ==================【6️⃣：数据库层防重复订单（第二道防线）】=================
            ParkingOrder existOrder = parkingOrderMapper.selectOne(
                    Wrappers.<ParkingOrder>lambdaQuery()
                            .eq(ParkingOrder::getPlateNo, plateNo)
                            .in(ParkingOrder::getStatus, "UNPAID", "PAID")
                            .orderByDesc(ParkingOrder::getCreateTime)
                            .last("LIMIT 1")
            );

            if (existOrder != null) {
                log.warn("【出闸拦截】车牌 {} 存在未完成订单 orderNo={}",
                        plateNo, existOrder.getOrderNo());

                ParkingGateExitResult result = new ParkingGateExitResult();
                result.setAllowPass(false);
                result.setNeedPay(true);
                result.setAmount(existOrder.getAmount());
                result.setOrderNo(existOrder.getOrderNo());
                result.setMessage("存在未完成订单，请先支付");
                return result;
            }

            // ==================【7️⃣：临时车计费逻辑】=================
            long minutes = Duration.between(enterTime, exitTime).toMinutes();
            long hours = minutes <= 0 ? 1 : (minutes + 59) / 60;
            BigDecimal amount = BigDecimal.valueOf(hours * 10);

            log.info("【计费信息】车牌 {} 停车时长 {} 分钟，计费 {} 元",
                    plateNo, minutes, amount);

            // ==================【8️⃣：生成唯一停车订单】=================
            String orderNo = generateOrderNo();
            ParkingOrder order = new ParkingOrder();
            order.setOrderNo(orderNo);
            order.setPlateNo(plateNo);
            order.setUserId(enterLog.getUserId() == null ? 0L : enterLog.getUserId());

            // ✅ 关键修复：设置 spaceId（车位ID）
            // 从入闸记录获取车位ID，如果入闸时没有分配车位，可以设置为0或null
            if (enterLog.getSpaceId() != null) {
                order.setSpaceId(enterLog.getSpaceId());
            } else {
                // 临时车可能没有固定车位，根据数据库约束决定
                // 如果数据库不允许null，设置为0
                order.setSpaceId(0L);
                // 或者如果数据库允许null：order.setSpaceId(null);
            }

            order.setOrderType("TEMP");
            order.setAmount(amount);
            order.setStatus("UNPAID");
            order.setStartTime(enterTime);
            order.setEndTime(exitTime);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            parkingOrderMapper.insert(order);

            log.info("【订单生成】车牌 {} 生成停车订单 orderNo={}, amount={}",
                    plateNo, orderNo, amount);

            // ==================【9️⃣：返回支付信息（临时车不放行）】=================
            ParkingGateExitResult result = new ParkingGateExitResult();
            result.setAllowPass(false);
            result.setNeedPay(true);
            result.setAmount(amount);
            result.setOrderNo(orderNo);
            result.setMessage("请支付停车费后出闸");

            return result;

        } catch (Exception e) {
            // 记录业务异常日志
            log.error("【出闸异常】车牌 {} 处理出闸逻辑时发生异常", plateNo, e);
            throw e;
        } finally {
            // ==================【🔟：释放 Redis 锁】=================
            try {
                redisLockUtil.unlock(lockKey, lockValue);
                log.debug("【锁释放】车牌 {} 成功释放分布式锁", plateNo);
            } catch (Exception e) {
                log.error("【锁释放异常】车牌 {} 释放分布式锁失败", plateNo, e);
                // 这里不抛出异常，避免掩盖业务异常
            }
        }
    }

    private String generateOrderNo() {
        // 格式：P + 年月日时分秒 + 4位随机数
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "P" + timePart + randomPart;
    }

    private void recordExitLog(String plateNo,
                               ParkingGateLog enterLog,
                               String gateType,
                               String remark) {

        ParkingGateLog exitLog = new ParkingGateLog();
        exitLog.setPlateNo(plateNo);
        exitLog.setUserId(enterLog.getUserId());
        exitLog.setSpaceId(enterLog.getSpaceId());
        exitLog.setGateType(gateType);
        exitLog.setAction("EXIT");
        exitLog.setResult("SUCCESS");
        exitLog.setRemark(remark);
        exitLog.setCreateTime(LocalDateTime.now());

        gateLogMapper.insert(exitLog);
    }
}
