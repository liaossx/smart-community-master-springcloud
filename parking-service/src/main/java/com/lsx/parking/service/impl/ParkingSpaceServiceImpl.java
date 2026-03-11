package com.lsx.parking.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.ParkingAuthorizeDTO;
import com.lsx.parking.dto.ParkingSpaceBindDTO;
import com.lsx.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.parking.entity.ParkingAuthorize;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingAuthorizeMapper;
import com.lsx.parking.mapper.ParkingPlateMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingAuthorizeVO;
import com.lsx.parking.vo.ParkingSpaceRemainVO;
import com.lsx.parking.vo.ParkingSpaceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.lsx.parking.dto.SpaceRenewDTO;
import com.lsx.parking.dto.SpaceOpenDTO;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import com.lsx.parking.service.ParkingAccountService;

import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingOrderMapper;
import cn.hutool.core.util.RandomUtil;
import java.time.format.DateTimeFormatter;

@Service
public class ParkingSpaceServiceImpl
        extends ServiceImpl<ParkingSpaceMapper, ParkingSpace>
        implements ParkingSpaceService {

    @Autowired
    private ParkingOrderMapper parkingOrderMapper;


    @Autowired
    private ParkingAccountService parkingAccountService;

    // ... (existing fields)

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openSpace(SpaceOpenDTO dto) {
        Assert.notNull(dto.getSpaceId(), "车位ID/记录ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.notNull(dto.getAmount(), "鏀粯金额不能为空");
        Assert.notNull(dto.getDurationMonths(), "璐拱时长不能为空");

        // 1. 鏌ユ壘待缴费圭殑绑定记录
        // 灏濊瘯浣滀负 plateId 查询
        ParkingSpacePlate plate = plateMapper.selectById(dto.getSpaceId());
        
        // 濡傛灉鎸塈D娌℃煡鍒帮紝鎴栬€呮煡鍒扮殑记录状态€佷笉瀵?用户涓嶅锛屽皾璇曟寜 spaceId + userId 查询
        if (plate == null || !plate.getUserId().equals(dto.getUserId())) {
            plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                    .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                    .eq(ParkingSpacePlate::getUserId, dto.getUserId())
                    .eq(ParkingSpacePlate::getStatus, "AWAITING_PAYMENT")
                    .last("limit 1"));
        }
        
        if (plate == null) {
             throw new RuntimeException("鏈壘鍒板緟缴费鐨勭粦瀹氳褰?);
        }
        
        if (!"AWAITING_PAYMENT".equals(plate.getStatus())) {
             throw new RuntimeException("璇ヨ褰曠姸鎬佷笉鏄緟缴费状态€?);
        }

        // 2. 所有ｉ櫎余额 (biz_parking_account)
        // 使用中 plateId 浣滀负鍏宠仈订单ID锛堟垨绋嶅悗创建鐨?leaseId锛?        // 杩欓噷鏆傛椂浼?plate.getId()锛屼篃鍙€冭檻浼?0 鎴栧叾浠栨爣璇?        try {
            parkingAccountService.consume(dto.getUserId(), dto.getAmount(), plate.getId());
        } catch (Exception e) {
             throw new RuntimeException("余额涓嶈冻锛岃充值?);
        }
        
        // 3. 更新绑定记录状态€?        plate.setStatus("ACTIVE");
        plate.setUpdateTime(LocalDateTime.now());
        plateMapper.updateById(plate);
        
        // 4. 更新/创建租赁记录
        ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                .eq(ParkingSpaceLease::getSpaceId, plate.getSpaceId())
                .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                .last("limit 1"));
        
        LocalDateTime now = LocalDateTime.now();
        if (lease == null) {
            lease = new ParkingSpaceLease();
            lease.setSpaceId(plate.getSpaceId());
            lease.setUserId(dto.getUserId());
            lease.setLeaseType("MONTHLY"); // 榛樿涓烘湀绉?            lease.setStartTime(now);
            lease.setCreateTime(now);
        }
        
        // 更新有效鏈?        lease.setStartTime(now);
        lease.setEndTime(now.plusMonths(dto.getDurationMonths()));
        lease.setStatus("ACTIVE");
        lease.setUpdateTime(now);
        
        if (lease.getId() == null) {
            leaseMapper.insert(lease);
        } else {
            leaseMapper.updateById(lease);
        }

        // 5. 鎻掑叆缁熶竴订单记录 (ParkingOrder)
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo("PK" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(4));
        order.setUserId(dto.getUserId());
        order.setSpaceId(plate.getSpaceId());
        order.setPlateNo(plate.getPlateNo());
        order.setOrderType("MONTHLY"); // 鎴栨牴鎹椂闀垮垽鏂?YEARLY
        if (dto.getDurationMonths() >= 12) {
             order.setOrderType("YEARLY");
        }
        order.setAmount(dto.getAmount());
        order.setStatus("PAID");
        order.setPayTime(now);
        order.setPayChannel(dto.getPayMethod());
        order.setPayRemark("棣栨开始€閫氳溅浣? " + plate.getPlateNo() + ", 时长: " + dto.getDurationMonths() + "涓湀");
        
        // 鑾峰彇社区ID
        ParkingSpace space = this.getById(plate.getSpaceId());
        if (space != null) {
            order.setCommunityId(space.getCommunityId());
        }
        
        order.setStartTime(now);
        order.setEndTime(lease.getEndTime());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        
        parkingOrderMapper.insert(order);

        // 6. 璋冪敤道闸系统接口 (妯℃嫙)
        // log.info("璋冪敤道闸系统下发车牌权限: {}", plate.getPlateNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewSpace(SpaceRenewDTO dto) {
        Assert.notNull(dto.getSpaceId(), "车位ID不能为空");
        Assert.notNull(dto.getDurationMonths(), "缁垂时长不能为空");
        Assert.notNull(dto.getAmount(), "鏀粯金额不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 1. 鏍￠獙车位鍜岀璧佸叧绯?        ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                .eq(ParkingSpaceLease::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                .last("limit 1"));

        if (lease == null) {
            // 濡傛灉鏄娆＄璧侊紙娌℃湁鍘嗗彶记录锛夛紝鍙兘闇€瑕佹柊寤?Lease
            // 杩欓噷鍋囪蹇呴』鍏堟湁租赁记录锛堝嵆浣胯繃鏈熺殑锛夋墠鑳界画璐?            // 濡傛灉瑕佹敮鎸佹柊璐紝閫昏緫浼氫笉鍚?             throw new RuntimeException("鏈壘鍒拌车位鐨勭璧佽褰曪紝璇疯仈绯荤鐞嗗憳开始€閫?);
        }

        // 2. 所有ｉ櫎余额 (biz_parking_account)
        // 使用中 lease.getId() 浣滀负鍏宠仈ID
        try {
            parkingAccountService.consume(dto.getUserId(), dto.getAmount(), lease.getId());
        } catch (Exception e) {
             throw new RuntimeException("余额涓嶈冻锛岃充值?);
        }

        // 3. 更新租赁时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newEndTime;
        
        // 鑾峰彇褰撳墠结束时间锛屽鏋滀负 null 鍒欒涓哄凡杩囨湡
        LocalDateTime currentEndTime = lease.getEndTime();
        
        if (currentEndTime == null || currentEndTime.isBefore(now)) {
            // 已过期熸垨鏃犳埅姝㈡椂闂达紝浠庣幇鍦ㄥ紑濮嬭绠?            lease.setStartTime(now); // 閲嶆柊婵€娲?            newEndTime = now.plusMonths(dto.getDurationMonths());
        } else {
            // 鏈繃鏈燂紝缁湪鍚庨潰
            newEndTime = currentEndTime.plusMonths(dto.getDurationMonths());
        }
        
        lease.setEndTime(newEndTime);
        lease.setStatus("ACTIVE");
        lease.setUpdateTime(now);
        
        leaseMapper.updateById(lease);
        
        // 4. 鍚屾更新车位状态€侊紙濡傛灉涔嬪墠鏄?DISABLED 鎴栧叾浠栫姸鎬侊級
        ParkingSpace space = this.getById(dto.getSpaceId());
        if (space != null) {
            // 固定车位琚璧佸悗锛岄€氬父状态€佽涓?DISABLED (鍗犵敤)
            if (!"DISABLED".equals(space.getStatus())) {
                space.setStatus("DISABLED");
                space.setUpdateTime(now);
                this.updateById(space);
            }
        }
        
        // 5. 鎻掑叆缁熶竴订单记录 (ParkingOrder)
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo("PK" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(4));
        order.setUserId(dto.getUserId());
        order.setSpaceId(dto.getSpaceId());
        
        // 灏濊瘯鑾峰彇车牌鍙?        String plateNo = "";
        ParkingSpacePlate plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpacePlate::getUserId, dto.getUserId())
                .last("limit 1"));
        if (plate != null) {
            plateNo = plate.getPlateNo();
        }
        order.setPlateNo(plateNo);
        
        order.setOrderType("MONTHLY"); // 鎴栨牴鎹椂闀垮垽鏂?YEARLY
        if (dto.getDurationMonths() >= 12) {
             order.setOrderType("YEARLY");
        }
        order.setAmount(dto.getAmount());
        order.setStatus("PAID");
        order.setPayTime(now);
        order.setPayChannel("BALANCE"); // 缁垂閫氬父涔熸槸余额
        order.setPayRemark("车位缁垂: " + plateNo + ", 时长: " + dto.getDurationMonths() + "涓湀");
        
        if (space != null) {
            order.setCommunityId(space.getCommunityId());
        }
        
        // 缁垂鐨勮捣濮嬫椂闂村簲璇ユ槸涔嬪墠鐨勭粨鏉熸椂闂达紵鎴栬€呮槸鐜板湪锛?        // 杩欓噷绠€鍗曞鐞嗕负鏈缴费瑕嗙洊鐨勬椂闂存
        // 娉ㄦ剰锛歳enewSpace 鏂规硶涓凡缁忚绠椾簡 newEnd锛屼絾杩欓噷鎴戜滑鏃犳硶鐩存帴鑾峰彇涔嬪墠鐨?end
        // 涓轰簡绠€鍖栵紝startTime 璁句负 now (濡傛灉涓嶄弗璋?锛屾垨鑰呰涓?lease.getStartTime() (涔熶笉瀵?
        // 姣旇緝涓ヨ皑鐨勬槸锛氬鏋滄槸缁湡锛宻tartTime 搴旇鏄?涔嬪墠鐨?endTime銆?        // 浣嗙敱浜?newEndTime 宸茬粡更新鍒?lease 瀵硅薄閲屼簡锛屾垜浠棤娉曡交鏄撳緱鐭モ€滆繖涓€绗斺€濆鍔犵殑时间娈?        // 鏆備笖灏?startTime 璁句负 now锛宔ndTime 璁句负 newEndTime
        order.setStartTime(now); 
        order.setEndTime(newEndTime);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        
        parkingOrderMapper.insert(order);
    }

    // ... (existing methods)

    private static final String SPACE_AVAILABLE = "AVAILABLE";
    private static final String SPACE_FIXED = "FIXED";

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAuthorizeMapper parkingAuthorizeMapper;

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingPlateMapper plateMapper;

    /**
     * 查询剩余车位
     */
    @Override
    public ParkingSpaceRemainVO getRemaining(Long communityId) {

        long tempRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, "TEMP")
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        long fixedRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, SPACE_FIXED)
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        ParkingSpaceRemainVO vo = new ParkingSpaceRemainVO();
        vo.setCommunityId(communityId);
        vo.setTempRemaining(tempRemain);
        vo.setFixedRemaining(fixedRemain);

        if (communityId != null) {
            ParkingSpace space = this.getOne(
                    Wrappers.<ParkingSpace>lambdaQuery()
                            .eq(ParkingSpace::getCommunityId, communityId)
                            .last("limit 1")
            );
            if (space != null) {
                vo.setCommunityName(space.getCommunityName());
            }
        }
        return vo;
    }

    /**
     * 绑定固定车位锛堝彧鍋氣€滃崰浣嶁€濓紝涓嶄骇鐢熶娇鐢ㄦ潈锛?     */
    @Override
    public Boolean bindSpace(ParkingSpaceBindDTO dto) {

        Assert.notNull(dto.getSpaceId(), "车位ID不能为空");

        ParkingSpace space = this.getById(dto.getSpaceId());
        if (space == null) {
            throw new RuntimeException("车位不存在?);
        }
        if (!SPACE_AVAILABLE.equals(space.getStatus())) {
            throw new RuntimeException("车位涓嶅彲绑定");
        }
        if (!SPACE_FIXED.equals(space.getSpaceType())) {
            throw new RuntimeException("浠呭浐瀹氳溅浣嶅彲绑定");
        }

        // 浠呮敼鍙樿溅浣嶇姸鎬?        space.setStatus("DISABLED");
        space.setUpdateTime(LocalDateTime.now());

        return this.updateById(space);
    }

    /**
     * 查询鎴戞嫢鏈夌殑车位锛堝熀浜庝娇鐢ㄦ潈锛?     */
    @Override
    public List<ParkingSpaceVO> listMySpaces(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");

        // 查询用户鐩稿叧鐨勮溅杈嗙粦瀹氳褰?(PENDING, AWAITING_PAYMENT, ACTIVE)
        List<ParkingSpacePlate> plates = plateMapper.selectList(
                Wrappers.<ParkingSpacePlate>lambdaQuery()
                        .eq(ParkingSpacePlate::getUserId, userId)
                        .in(ParkingSpacePlate::getStatus, "PENDING", "AWAITING_PAYMENT", "ACTIVE")
                        .orderByDesc(ParkingSpacePlate::getCreateTime)
        );

        LocalDateTime now = LocalDateTime.now();

        return plates.stream().map(plate -> {
            ParkingSpace space = parkingSpaceMapper.selectById(plate.getSpaceId());
            if (space == null) return null;

            ParkingSpaceVO vo = new ParkingSpaceVO();
            vo.setId(space.getId());
            vo.setSlot(space.getSpaceNo());
            vo.setSpaceNo(space.getSpaceNo());
            vo.setCommunityName(space.getCommunityName());
            vo.setPlateNo(plate.getPlateNo());

            // 状态€佸垽鏂?            String plateStatus = plate.getStatus();
            
            // 灏濊瘯鏌ユ壘 Lease 淇℃伅
            ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                    .eq(ParkingSpaceLease::getSpaceId, plate.getSpaceId())
                    .eq(ParkingSpaceLease::getUserId, userId)
                    .last("limit 1"));

            if (lease != null) {
                vo.setLeaseType(lease.getLeaseType());
                vo.setLeaseStartTime(lease.getStartTime());
                vo.setLeaseEndTime(lease.getEndTime());
                
                // 濡傛灉鏄?ACTIVE 状态€侊紝杩涗竴姝ユ鏌ユ椂闂存槸鍚﹁繃鏈?                if ("ACTIVE".equals(plateStatus)) {
                    boolean isLeaseActive = lease.getEndTime() != null && lease.getEndTime().isAfter(now);
                    vo.setLeaseStatus(isLeaseActive ? "ACTIVE" : "EXPIRED");
                    vo.setActive(isLeaseActive);
                    vo.setStatusText(isLeaseActive ? "使用中? : "已过期?);
                } else {
                    // 闈?ACTIVE 状态€佺洿鎺ヤ娇鐢?Plate 状态€?                    vo.setLeaseStatus(plateStatus);
                    vo.setActive(false);
                    if ("PENDING".equals(plateStatus)) vo.setStatusText("瀹℃牳涓?);
                    else if ("AWAITING_PAYMENT".equals(plateStatus)) vo.setStatusText("待缴费?);
                }
            } else {
                // 鏃?Lease 记录
                vo.setLeaseStatus(plateStatus);
                vo.setActive(false);
                if ("PENDING".equals(plateStatus)) vo.setStatusText("瀹℃牳涓?);
                else if ("AWAITING_PAYMENT".equals(plateStatus)) vo.setStatusText("待缴费?);
                else if ("ACTIVE".equals(plateStatus)) vo.setStatusText("使用中?鏃犵璧佽褰?");
            }

            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    /**
     * 固定车位授权璁垮
     */
    @Override
    public Boolean authorizeSpace(Long spaceId, ParkingAuthorizeDTO dto) {

        Assert.notNull(spaceId, "车位ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 鏍￠獙鏄惁鏈夋湁鏁堜娇鐢ㄦ潈
        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getSpaceId, spaceId)
                        .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("limit 1")
        );

        if (lease == null) {
            throw new RuntimeException("鏃犺车位鐨勬湁鏁堜娇鐢ㄦ潈");
        }

        ParkingAuthorize authorize = new ParkingAuthorize();
        authorize.setSpaceId(spaceId);
        authorize.setUserId(dto.getUserId());
        authorize.setAuthorizedName(dto.getAuthorizedName());
        authorize.setAuthorizedPhone(dto.getAuthorizedPhone());
        authorize.setPlateNo(dto.getPlateNo());
        authorize.setStartTime(LocalDateTime.now());
        authorize.setEndTime(dto.getEndTime());
        authorize.setStatus("ACTIVE");
        authorize.setCreateTime(LocalDateTime.now());
        authorize.setUpdateTime(LocalDateTime.now());

        parkingAuthorizeMapper.insert(authorize);
        return true;
    }

    /**
     * 查询鎴戠殑授权记录
     */
    @Override
    public IPage<ParkingAuthorizeVO> listMyAuthorizes(
            Long userId, Integer pageNum, Integer pageSize) {

        Page<ParkingAuthorize> page = new Page<>(pageNum, pageSize);

        IPage<ParkingAuthorize> authorizePage =
                parkingAuthorizeMapper.selectPage(page,
                        Wrappers.<ParkingAuthorize>lambdaQuery()
                                .eq(ParkingAuthorize::getUserId, userId)
                                .orderByDesc(ParkingAuthorize::getCreateTime));

        return authorizePage.convert(record -> {
            ParkingAuthorizeVO vo = new ParkingAuthorizeVO();
            BeanUtil.copyProperties(record, vo);

            ParkingSpace space = this.getById(record.getSpaceId());
            if (space != null) {
                vo.setSpaceNo(space.getSpaceNo());
            }

            if (record.getEndTime() != null &&
                    record.getEndTime().isBefore(LocalDateTime.now())) {
                vo.setStatus("EXPIRED");
            } else {
                vo.setStatus(record.getStatus());
            }
            return vo;
        });
    }

    @Override
    public IPage<ParkingSpaceVO> adminListSpaces(ParkingSpaceQueryDTO dto) {
        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long currentCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                // 鏃犵ぞ鍖哄垯鏌ヤ笉鍒颁换浣曟暟鎹?                dto.setCommunityId(-1L);
            } else {
                // 开始哄埗闄愬畾涓哄綋鍓嶇ぞ鍖?                dto.setCommunityId(currentCommunityId);
            }
        }
        Page<ParkingSpaceVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return parkingSpaceMapper.selectAdminPage(page, dto);
    }

    @Override
    public IPage<ParkingSpaceVO> listAvailableFixedSpaces(Long communityId, Integer pageNum, Integer pageSize) {
        Page<ParkingSpace> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ParkingSpace> query = Wrappers.lambdaQuery();
        query.eq(ParkingSpace::getStatus, SPACE_AVAILABLE)
             .eq(ParkingSpace::getSpaceType, SPACE_FIXED);
             
        if (communityId != null) {
            query.eq(ParkingSpace::getCommunityId, communityId);
        }
        
        IPage<ParkingSpace> spacePage = parkingSpaceMapper.selectPage(page, query);
        
        return spacePage.convert(space -> {
            ParkingSpaceVO vo = new ParkingSpaceVO();
            BeanUtil.copyProperties(space, vo);
            vo.setSlot(space.getSpaceNo());
            return vo;
        });
    }
}


