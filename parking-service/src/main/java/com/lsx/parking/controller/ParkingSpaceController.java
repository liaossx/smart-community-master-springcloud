package com.lsx.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.parking.dto.ParkingAuthorizeDTO;
import com.lsx.parking.dto.ParkingSpaceBindDTO;
import com.lsx.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingAuthorizeVO;
import com.lsx.parking.vo.ParkingSpaceRemainVO;
import com.lsx.parking.vo.ParkingSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lsx.parking.dto.SpaceRenewDTO;

import com.lsx.parking.dto.SpaceOpenDTO;

@RestController
@RequestMapping("/api/parking")
@Tag(name = "车位管理员接口")
@Slf4j
public class ParkingSpaceController {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @PostMapping("/space/open")
    @Operation(summary = "车辆/车位棣栨开始€閫氱即璐?, description = "鐢ㄤ簬待缴费圭姸鎬佷笅鐨勯娆℃敮浠樺紑閫?)
    public Result<Boolean> openSpace(@RequestBody SpaceOpenDTO dto) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null && dto.getUserId() == null) {
                return Result.fail("鏈櫥褰?);
            }
            if (userId != null) {
                dto.setUserId(userId);
            }
            
            parkingSpaceService.openSpace(dto);
            return Result.success(true);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("开始€閫氳溅浣嶅紓甯?, e);
            return Result.fail("开始€閫氬け璐?);
        }
    }

    @PostMapping("/space/renew")
    @Operation(summary = "车位缁垂", description = "鏀粯金额骞跺欢闀挎湁鏁堟湡")
    public Result<Boolean> renewSpace(@RequestBody SpaceRenewDTO dto) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null && dto.getUserId() == null) {
                return Result.fail("鏈櫥褰?);
            }
            // 浼樺厛使用中 Token 涓殑 userId
            if (userId != null) {
                dto.setUserId(userId);
            }
            
            parkingSpaceService.renewSpace(dto);
            return Result.success(true);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("缁垂异常", e);
            return Result.fail("缁垂失败");
        }
    }

    @GetMapping("/space/remaining")
    @Operation(summary = "查询鍙敤车位鏁伴噺", description = "鏀寔鎸夊皬鍖篒D查询锛屾湭浼犲垯统计全部")
    public Result<ParkingSpaceRemainVO> getRemaining(@RequestParam(value = "communityId", required = false) Long communityId) {
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            communityId = currentCommunityId;
        }
        ParkingSpaceRemainVO vo = parkingSpaceService.getRemaining(communityId);
        return Result.success(vo);
    }

    @PostMapping("/space/bind")
    @Operation(summary = "业主绑定固定车位", description = "绑定鍚庤溅浣嶇姸鎬佸彉涓哄崰鐢?)
    public Result<Boolean> bindSpace(@RequestBody ParkingSpaceBindDTO dto) {
        try {
            Boolean success = parkingSpaceService.bindSpace(dto);
            return success ? Result.success(true) : Result.fail("绑定失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("绑定车位异常", e);
            return Result.fail("绑定失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/space/my")
    @Operation(summary = "查询鎴戠粦瀹氱殑车位")
    public Result<List<ParkingSpaceVO>> listMySpaces() {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.fail("鏈櫥褰?);
            }
            List<ParkingSpaceVO> spaces = parkingSpaceService.listMySpaces(userId);
            return Result.success(spaces);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询车位失败", e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @PostMapping("/space/{id}/authorize")
    @Operation(summary = "固定车位授权璁垮使用中")
    public Result<Boolean> authorize(@PathVariable("id") Long spaceId, @RequestBody ParkingAuthorizeDTO dto) {
        try {
            Boolean success = parkingSpaceService.authorizeSpace(spaceId, dto);
            return success ? Result.success(true) : Result.fail("授权失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车位授权异常", e);
            return Result.fail("授权失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/authorize/my")
    @Operation(summary = "业主查询鑷繁鐨勬巿鏉冭褰?)
    public Result<IPage<ParkingAuthorizeVO>> listMyAuthorize(@RequestParam("userId") Long userId,
                                                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            IPage<ParkingAuthorizeVO> page = parkingSpaceService.listMyAuthorizes(userId, pageNum, pageSize);
            return Result.success(page);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询授权记录异常", e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/space/admin/list")
    @Operation(summary = "管理员樿幏鍙栬溅浣嶅垪琛?, description = "鏀寔鎸夌紪鍙锋煡璇紝返回鏄惁鍗犵敤鍙婁笟涓讳俊鎭?)
    public Result<Map<String, Object>> getAdminSpaceList(ParkingSpaceQueryDTO dto) {
        try {
            IPage<ParkingSpaceVO> page = parkingSpaceService.adminListSpaces(dto);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("管理员樻煡璇㈣溅浣嶅垪琛ㄥ紓甯?, e);
            return Result.fail("查询失败");
        }
    }

    @GetMapping("/space/available")
    @Operation(summary = "查询鍙敤固定车位鍒楄〃", description = "鐢ㄤ簬业主绑定固定车位鏃堕€夋嫨锛屼粎返回状态€佷负AVAILABLE涓旂被鍨嬩负FIXED鐨勮溅浣?)
    public Result<IPage<ParkingSpaceVO>> listAvailableFixedSpaces(
            @RequestParam(required = false) Long communityId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 濡傛灉鏈紶 communityId锛屽皾璇曚粠褰撳墠用户鑾峰彇
        if (communityId == null) {
            communityId = UserContext.getCommunityId();
        }
        
        IPage<ParkingSpaceVO> page = parkingSpaceService.listAvailableFixedSpaces(communityId, pageNum, pageSize);
        return Result.success(page);
    }
}



