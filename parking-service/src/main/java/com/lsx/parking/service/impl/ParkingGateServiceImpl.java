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
            throw new RuntimeException("车牌鍙蜂笉鑳戒负绌?);
        }

        String plateNo = dto.getPlateNo();  // 杩欓噷鑾峰彇浜嗚溅鐗屽彿

        // 1锔忊儯 鏍规嵁车牌查询溅杈?        Vehicle vehicle = vehicleMapper.selectByPlateNo(plateNo);

        boolean isOwnerVehicle = vehicle != null;
        ParkingSpaceLease lease = null;

        if (isOwnerVehicle) {
            // 2锔忊儯 鏌ユ槸鍚︽湁有效车位使用中鏉冿紙鍖呮湀 / 固定锛?            lease = leaseMapper.selectActiveLeaseByUser(vehicle.getUserId());
        }

        // 3锔忊儯 记录鍏ラ椄鏃ュ織
        ParkingGateLog log = new ParkingGateLog();
        log.setUserId(isOwnerVehicle ? vehicle.getUserId() : null);
        log.setSpaceId(lease != null ? lease.getSpaceId() : null);
        log.setGateType(isOwnerVehicle ? "OWNER" : "TEMP");
        log.setAction("ENTER");
        log.setResult("SUCCESS");

        // 猸愨瓙猸?鏂板锛氳缃溅鐗屽彿 猸愨瓙猸?        log.setPlateNo(plateNo);  // 杩欓噷蹇呴』璁剧疆锛?        if (!isOwnerVehicle) {
            log.setRemark("澶栨潵车辆鍏ラ椄");
        } else if (lease != null) {
            log.setRemark("业主固定车位鍏ラ椄");
        } else {
            log.setRemark("业主临时鍋滆溅鍏ラ椄");
        }

        log.setCreateTime(LocalDateTime.now());
        gateLogMapper.insert(log);

        // 4锔忊儯 鏄惁鏀捐锛?        // 璇存槑锛?        // - 固定车位 / 鍖呮湀锛氱洿鎺ユ斁琛?        // - 业主涓村仠 / 澶栨潵车辆锛氫篃鍏佽鍏ラ椄锛屽嚭闂告椂绠楅挶
    }

    @Override
    @Transactional
    public ParkingGateExitResult exitGate(ParkingGateExitDTO dto) {
        String plateNo = dto.getPlateNo();

        // ==================銆?锔忊儯锛歊edis 鍒嗗竷开始忛攣閰嶇疆銆?=================
        // Key锛氬悓涓€杈嗚溅锛堝悓涓€车牌锛夊湪鍚屼竴鏃跺埢鍙兘鏈変竴涓嚭闂哥嚎绋?        String lockKey = "lock:parking:exit:" + plateNo;
        // Value锛氱敤浜庤В销毁佹椂鏍￠獙锛岄槻姝㈣鍒犲埆浜虹殑销毁?        String lockValue = UUID.randomUUID().toString();

        // ==================銆?锔忊儯锛氬甫閲嶈瘯鐨勫垎甯冨紡销毁佽幏鍙栥€?=================
        // 閰嶇疆参数锛氶攣10绉掕繃鏈燂紝鏈€澶氶噸璇?娆★紝姣忔筛选夊緟100姣
        boolean locked = false;
        try {
            locked = redisLockUtil.tryLockWithRetry(lockKey, lockValue, 10, 3, 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("銆愬嚭闂镐腑鏂€戣溅鐗?{} 鑾峰彇销毁佽涓柇", plateNo, e);
            throw new RuntimeException("系统绻佸繖锛岃绋嶅悗閲嶈瘯");
        }

        if (!locked) {
            // 馃毀 澶氭閲嶈瘯鍚庝粛鏃犳硶鑾峰彇销毁侊紝璇存槑璇ヨ溅杈嗘鍦ㄨ其他璇锋眰澶勭悊
            log.warn("銆愬嚭闂稿啿绐併€戣溅鐗?{} 澶氭灏濊瘯鑾峰彇销毁佸け璐ワ紝鍙兘琚伓鎰忛噸澶嶈姹?, plateNo);
            throw new RuntimeException("车辆姝ｅ湪鍑洪椄澶勭悊涓紝璇风◢鍚庨噸璇?);
        }

        try {
            log.info("銆愬嚭闂稿紑濮嬨€戣溅鐗?{} 成功鑾峰彇鍒嗗竷开始忛攣锛屽紑濮嬪鐞嗗嚭闂搁€昏緫", plateNo);

            // ==================銆?锔忊儯锛氬熀纭€参数鏍￠獙銆?=================
            if (!StringUtils.hasText(plateNo)) {
                throw new RuntimeException("车牌鍙蜂笉鑳戒负绌?);
            }

            LocalDateTime exitTime = LocalDateTime.now();

            // ==================銆?锔忊儯锛氭煡璇㈡渶杩戜竴娆″叆闂歌褰曘€?=================
            ParkingGateLog enterLog = gateLogMapper.selectOne(
                    Wrappers.<ParkingGateLog>lambdaQuery()
                            .eq(ParkingGateLog::getPlateNo, plateNo)
                            .eq(ParkingGateLog::getAction, "ENTER")
                            .orderByDesc(ParkingGateLog::getCreateTime)
                            .last("LIMIT 1")
            );

            if (enterLog == null) {
                log.warn("銆愬嚭闂稿け璐ャ€戣溅鐗?{} 鏈壘鍒板叆闂歌褰?, plateNo);
                throw new RuntimeException("鏈壘鍒板叆闂歌褰曪紝鏃犳硶鍑洪椄");
            }

            LocalDateTime enterTime = enterLog.getCreateTime();

            // ==================銆?锔忊儯锛氬浐瀹氳溅浣嶆牎楠屻€?=================
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

            // ==================銆?锔忊儯锛氬浐瀹氳溅浣?鈫?鐩存帴鏀捐銆?=================
            if (isFixedOwner) {
                log.info("銆愬浐瀹氳溅浣嶆斁琛屻€戣溅鐗?{} 涓哄浐瀹氳溅浣嶄笟涓伙紝鐩存帴鏀捐", plateNo);

                ParkingGateLog exitLog = new ParkingGateLog();
                exitLog.setPlateNo(plateNo);
                exitLog.setUserId(enterLog.getUserId());
                exitLog.setSpaceId(lease.getSpaceId());
                exitLog.setGateType("FIXED");
                exitLog.setAction("EXIT");
                exitLog.setResult("SUCCESS");
                exitLog.setRemark("固定车位业主鐩存帴鏀捐");
                exitLog.setCreateTime(exitTime);
                gateLogMapper.insert(exitLog);

                ParkingGateExitResult result = new ParkingGateExitResult();
                result.setAllowPass(true);
                result.setNeedPay(false);
                result.setMessage("固定车位业主锛岀洿鎺ユ斁琛?);
                return result;
            }

            // ==================銆?锔忊儯锛氭暟鎹簱灞傞槻閲嶅订单锛堢浜岄亾闃茬嚎锛夈€?=================
            ParkingOrder existOrder = parkingOrderMapper.selectOne(
                    Wrappers.<ParkingOrder>lambdaQuery()
                            .eq(ParkingOrder::getPlateNo, plateNo)
                            .in(ParkingOrder::getStatus, "UNPAID", "PAID")
                            .orderByDesc(ParkingOrder::getCreateTime)
                            .last("LIMIT 1")
            );

            if (existOrder != null) {
                log.warn("銆愬嚭闂告嫤鎴€戣溅鐗?{} 瀛樺湪鏈畬鎴愯鍗?orderNo={}",
                        plateNo, existOrder.getOrderNo());

                ParkingGateExitResult result = new ParkingGateExitResult();
                result.setAllowPass(false);
                result.setNeedPay(true);
                result.setAmount(existOrder.getAmount());
                result.setOrderNo(existOrder.getOrderNo());
                result.setMessage("瀛樺湪鏈畬鎴愯鍗曪紝璇峰厛鏀粯");
                return result;
            }

            // ==================銆?锔忊儯锛氫复鏃惰溅璁¤垂閫昏緫銆?=================
            long minutes = Duration.between(enterTime, exitTime).toMinutes();
            long hours = minutes <= 0 ? 1 : (minutes + 59) / 60;
            BigDecimal amount = BigDecimal.valueOf(hours * 10);

            log.info("銆愯璐逛俊鎭€戣溅鐗?{} 鍋滆溅时长 {} 鍒嗛挓锛岃璐?{} 鍏?,
                    plateNo, minutes, amount);

            // ==================銆?锔忊儯锛氱敓鎴愬敮涓€鍋滆溅订单銆?=================
            String orderNo = generateOrderNo();
            ParkingOrder order = new ParkingOrder();
            order.setOrderNo(orderNo);
            order.setPlateNo(plateNo);
            order.setUserId(enterLog.getUserId() == null ? 0L : enterLog.getUserId());

            // 鉁?鍏抽敭淇锛氳缃?spaceId锛堣溅浣岻D锛?            // 浠庡叆闂歌褰曡幏鍙栬溅浣岻D锛屽鏋滃叆闂告椂娌℃湁鍒嗛厤车位锛屽彲浠ヨ缃负0鎴杗ull
            if (enterLog.getSpaceId() != null) {
                order.setSpaceId(enterLog.getSpaceId());
            } else {
                // 临时杞﹀彲鑳芥病鏈夊浐瀹氳溅浣嶏紝鏍规嵁鏁版嵁搴撶害鏉熷喅瀹?                // 濡傛灉鏁版嵁搴撲笉鍏佽null锛岃缃负0
                order.setSpaceId(0L);
                // 鎴栬€呭鏋滄暟鎹簱鍏佽null锛歰rder.setSpaceId(null);
            }

            order.setOrderType("TEMP");
            order.setAmount(amount);
            order.setStatus("UNPAID");
            order.setStartTime(enterTime);
            order.setEndTime(exitTime);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            parkingOrderMapper.insert(order);

            log.info("銆愯鍗曠敓鎴愩€戣溅鐗?{} 生成鍋滆溅订单 orderNo={}, amount={}",
                    plateNo, orderNo, amount);

            // ==================銆愷煍燂細返回鏀粯淇℃伅锛堜复鏃惰溅涓嶆斁琛岋級銆?=================
            ParkingGateExitResult result = new ParkingGateExitResult();
            result.setAllowPass(false);
            result.setNeedPay(true);
            result.setAmount(amount);
            result.setOrderNo(orderNo);
            result.setMessage("璇锋敮浠樺仠杞﹁垂鍚庡嚭闂?);

            return result;

        } catch (Exception e) {
            // 记录涓氬姟异常鏃ュ織
            log.error("銆愬嚭闂稿紓甯搞€戣溅鐗?{} 澶勭悊鍑洪椄閫昏緫鏃跺彂鐢熷紓甯?, plateNo, e);
            throw e;
        } finally {
            // ==================銆愷煍氾細閲婃斁 Redis 销毁併€?=================
            try {
                redisLockUtil.unlock(lockKey, lockValue);
                log.debug("銆愰攣閲婃斁銆戣溅鐗?{} 成功閲婃斁鍒嗗竷开始忛攣", plateNo);
            } catch (Exception e) {
                log.error("銆愰攣閲婃斁异常銆戣溅鐗?{} 閲婃斁鍒嗗竷开始忛攣失败", plateNo, e);
                // 杩欓噷涓嶆姏鍑哄紓甯革紝閬垮厤鎺╃洊涓氬姟异常
            }
        }
    }

    private String generateOrderNo() {
        // 鏍煎紡锛歅 + 骞存湀鏃ユ椂鍒嗙 + 4浣嶉殢鏈烘暟
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
