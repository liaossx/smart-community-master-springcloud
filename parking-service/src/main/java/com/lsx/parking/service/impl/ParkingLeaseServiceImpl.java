package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.parking.entity.ParkingAccount;
import com.lsx.parking.entity.ParkingLeaseOrder;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.mapper.ParkingAccountMapper;
import com.lsx.parking.mapper.ParkingLeaseOrderMapper;
import com.lsx.parking.mapper.ParkingOrderMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.service.ParkingLeaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.mapper.ParkingSpaceMapper;

import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingPlateMapper;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ParkingLeaseServiceImpl implements ParkingLeaseService {

    @Autowired
    private ParkingLeaseOrderMapper leaseOrderMapper;

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAccountMapper accountMapper;

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingPlateMapper plateMapper;
    @Override

    public Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto) {
        Assert.notNull(dto.getUserId(), "用户不能为空");
        Assert.notNull(dto.getSpaceId(), "车位不能为空");
        Assert.notNull(dto.getLeaseType(), "租赁绫诲瀷不能为空");

        BigDecimal amount;
        switch (dto.getLeaseType()) {
            case "MONTHLY": amount = BigDecimal.valueOf(200); break;
            case "YEARLY": amount = BigDecimal.valueOf(2000); break;
            case "PERPETUAL": amount = BigDecimal.valueOf(20000); break;
            default: throw new RuntimeException("闈炴硶租赁绫诲瀷");
        }

        ParkingLeaseOrder order = new ParkingLeaseOrder();
        order.setUserId(dto.getUserId());
        order.setSpaceId(dto.getSpaceId());
        order.setLeaseType(dto.getLeaseType());
        order.setAmount(amount);
        order.setStatus("UNPAID");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        leaseOrderMapper.insert(order);
        
        // 馃啎 鏂板锛氬鏋滀紶鍏ヤ簡车牌鍙凤紝涓旇车位涓嬪皻鏈粦瀹氳车牌锛屽垯鑷姩创建绑定记录
        if (StringUtils.hasText(dto.getPlateNo())) {
            // 妫€查询车位涓嬫槸鍚﹀凡瀛樺湪璇ヨ溅鐗岀殑 ACTIVE 记录
            ParkingSpacePlate existPlate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                    .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                    .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                    .eq(ParkingSpacePlate::getStatus, "ACTIVE")
                    .last("LIMIT 1"));
            
            if (existPlate == null) {
                // 濡傛灉娌℃湁锛屽啀妫€鏌ユ槸鍚︽湁 PENDING 鎴?AWAITING_PAYMENT 鐨勮褰曪紝濡傛灉鏈夊垯更新锛屾病鏈夊垯鎻掑叆
                ParkingSpacePlate pendingPlate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                        .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                        .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                        .in(ParkingSpacePlate::getStatus, "PENDING", "AWAITING_PAYMENT")
                        .last("LIMIT 1"));
                
                if (pendingPlate != null) {
                    // 宸叉湁寰呭鏍?待缴费硅褰曪紝鐩存帴灏嗗叾更新涓?ACTIVE (瑙嗕负管理员樺姙鐞嗗嵆閫氳繃)
                    // 浣嗛€氬父搴旇筛选夊埌 鏀粯成功 鍚庡啀 ACTIVE銆傝繖閲屽厛淇濇寔鍘熺姸鎬佹垨璁句负 AWAITING_PAYMENT锛?                    // 鑰冭檻鍒拌繖鏄€滃垱寤鸿鍗曗€濋樁娈碉紝杩樻湭鏀粯銆?                    // 筛选栫暐锛氬鏋滃畬鍏ㄦ病鏈夎褰曪紝鍒欏垱寤轰竴鏉?status="AWAITING_PAYMENT" 鐨勮褰曘€?                    // 鏀粯成功鍚庯紝payLeaseOrder 浼氳礋璐ｅ鐞?lease锛屼絾 plate 鐨勭姸鎬佷篃闇€瑕佸湪閭ｈ竟鍚屾更新銆?                    // 涓轰簡绠€鍖栵紝杩欓噷鍏堝垱寤鸿褰曪紝状态€佽涓?AWAITING_PAYMENT銆?                    // 鐪熸鐨?ACTIVE 搴旇鍦ㄦ敮浠樺洖璋冧腑銆?                    // 浣?payLeaseOrder 鐩墠鍙鐞?lease銆傛垜浠渶瑕佸湪 payLeaseOrder 涓ˉ鍏?plate 鐨勫鐞嗛€昏緫銆?                    
                    // 杩欓噷浠呭仛记录创建/鍏宠仈锛屼笉鏀?ACTIVE銆?                    // 濡傛灉记录不存在紝创建涓€鏉?AWAITING_PAYMENT
                } else {
                    ParkingSpacePlate newPlate = new ParkingSpacePlate();
                    newPlate.setSpaceId(dto.getSpaceId());
                    newPlate.setUserId(dto.getUserId());
                    newPlate.setPlateNo(dto.getPlateNo());
                    newPlate.setStatus("AWAITING_PAYMENT"); // 寰呮敮浠?                    newPlate.setCreateTime(LocalDateTime.now());
                    newPlate.setUpdateTime(LocalDateTime.now());
                    plateMapper.insert(newPlate);
                    log.info("创建鏈堝崱订单鏃惰嚜鍔ㄥ垱寤鸿溅鐗岀粦瀹氳褰? {}, 车牌: {}", dto.getSpaceId(), dto.getPlateNo());
                }
            }
        }
        
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payLeaseOrder(ParkingLeaseOrderPayDTO dto) {

        ParkingLeaseOrder order = leaseOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在?);
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("订单宸叉敮浠?);
        }
        
        Long userId = order.getUserId();
        String payChannel = dto.getPayChannel();

        // 鍒ゆ柇鏄惁闇€瑕佽烦杩囦綑棰濇墸鍑?(CASH/WECHAT/ALIPAY 筛选夌嚎涓嬫垨澶栭儴娓犻亾)
        boolean isSkipBalance = "CASH".equalsIgnoreCase(payChannel)
                || "WECHAT".equalsIgnoreCase(payChannel)
                || "ALIPAY".equalsIgnoreCase(payChannel);

        if (!isSkipBalance) {
            // 1锔忊儯 查询璐︽埛余额
            ParkingAccount account = accountMapper.selectOne(
                    Wrappers.<ParkingAccount>lambdaQuery()
                            .eq(ParkingAccount::getUserId, userId)
            );
            if (account == null) {
                throw new RuntimeException("璐︽埛不存在?);
            }

            // 2锔忊儯 鏍￠獙余额
            if (account.getBalance().compareTo(order.getAmount()) < 0) {
                throw new RuntimeException("璐︽埛余额涓嶈冻");
            }

            // 3锔忊儯 所有ｅ噺余额
            account.setBalance(account.getBalance().subtract(order.getAmount()));
            accountMapper.updateById(account);
            log.info("用户 {} 使用中余额鏀粯鏈堝崱订单 {}", userId, order.getId());
        } else {
            log.info("用户 {} 使用中娓犻亾 {} 鏀粯鏈堝崱订单 {}锛岃烦杩囦綑棰濇墸鍑?, userId, payChannel, order.getId());
        }

        // 4锔忊儯 更新订单状态€?        order.setStatus("PAID");
        order.setPayChannel(payChannel);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        leaseOrderMapper.updateById(order);

        // 馃啎 鏂板锛氭敮浠樻垚鍔熷悗锛屽悓姝ュ皢 AWAITING_PAYMENT 鐨勮溅鐗岀粦瀹氳褰曟洿鏂颁负 ACTIVE
        // 灏濊瘯鏍规嵁 spaceId 鍜?userId 鏌ユ壘鏈€杩戠殑 AWAITING_PAYMENT 记录
        ParkingSpacePlate plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                .eq(ParkingSpacePlate::getSpaceId, order.getSpaceId())
                .eq(ParkingSpacePlate::getUserId, order.getUserId())
                .eq(ParkingSpacePlate::getStatus, "AWAITING_PAYMENT")
                .last("LIMIT 1"));
        
        if (plate != null) {
            plate.setStatus("ACTIVE");
            plate.setUpdateTime(LocalDateTime.now());
            plateMapper.updateById(plate);
            log.info("鏀粯鏈堝崱成功锛屾縺娲昏溅鐗岀粦瀹? {}", plate.getPlateNo());
        }

        // 馃啎 鏂板锛氭敮浠樻垚鍔熷悗锛屾洿鏂拌溅浣嶇姸鎬佷负 OCCUPIED
        if (order.getSpaceId() != null) {
            ParkingSpace space = parkingSpaceMapper.selectById(order.getSpaceId());
            if (space != null) {
                space.setStatus("OCCUPIED");
                space.setUpdateTime(LocalDateTime.now());
                parkingSpaceMapper.updateById(space);
                log.info("车位 {} 状态€佸凡更新涓?OCCUPIED", order.getSpaceId());
            }
        }

        // 5锔忊儯 查询鎴栧垱寤虹璧佽褰?        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getUserId, userId)
                        .eq(ParkingSpaceLease::getSpaceId, order.getSpaceId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("LIMIT 1")
        );
        
        LocalDateTime now = LocalDateTime.now();
        boolean isNewLease = false;

        if (lease == null) {
            // 濡傛灉娌℃壘鍒?active lease锛屽垯鏍规嵁订单淇℃伅鑷姩开始€閫?            lease = new ParkingSpaceLease();
            lease.setUserId(userId);
            lease.setSpaceId(order.getSpaceId());
            lease.setLeaseType(order.getLeaseType());
            lease.setStatus("ACTIVE");
            lease.setStartTime(now);
            lease.setSourceOrderId(order.getId());
            lease.setCreateTime(now);
            lease.setUpdateTime(now);
            isNewLease = true;
        } else {
            // 姘镐箙车位绂佹缁垂
            if ("PERPETUAL".equals(lease.getLeaseType())) {
                throw new RuntimeException("姘镐箙车位鏃犻渶缁垂");
            }
        }

        // 6锔忊儯 璁＄畻有效鏈熻捣濮嬪熀鍑嗘椂闂?        LocalDateTime baseTime;
        if (isNewLease) {
            baseTime = now;
        } else {
            // 缁垂鍦烘櫙锛氳嫢鏈繃鏈熷垯鍦ㄥ師鍒版湡时间鍚庤拷鍔狅紝鑻ュ凡杩囨湡鍒欎粠鐜板湪开始€濮?            baseTime = (lease.getEndTime() != null && lease.getEndTime().isAfter(now))
                    ? lease.getEndTime()
                    : now;
        }

        // 7锔忊儯 鏍规嵁订单绫诲瀷璁＄畻鏂扮殑结束时间
        LocalDateTime newEnd = null;
        switch (order.getLeaseType()) {
            case "MONTHLY":
                newEnd = baseTime.plusMonths(1);
                break;
            case "YEARLY":
                newEnd = baseTime.plusYears(1);
                break;
            case "PERPETUAL":
                newEnd = null; // 姘镐箙车位结束时间涓?NULL
                break;
            default:
                throw new RuntimeException("涓嶆敮鎸佺殑租赁绫诲瀷: " + order.getLeaseType());
        }

        lease.setEndTime(newEnd);
        lease.setUpdateTime(now);

        // 8锔忊儯 淇濆瓨鎴栨洿鏂扮璧佽褰?        if (isNewLease) {
            leaseMapper.insert(lease);
            log.info("用户 {} 鑷姩开始€閫氳溅浣嶇璧? {}, 绫诲瀷: {}", userId, order.getSpaceId(), order.getLeaseType());
        } else {
            leaseMapper.updateById(lease);
            log.info("用户 {} 缁垂车位租赁: {}, 鏂板埌鏈熸椂闂? {}", userId, order.getSpaceId(), newEnd);
        }
    }
}

