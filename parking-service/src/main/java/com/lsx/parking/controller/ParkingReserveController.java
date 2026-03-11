package com.lsx.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingReserveAdminCancelDTO;
import com.lsx.parking.dto.ParkingReserveCancelDTO;
import com.lsx.parking.dto.ParkingReserveCreateDTO;
import com.lsx.parking.dto.ParkingReserveQueryDTO;
import com.lsx.parking.service.ParkingReserveService;
import com.lsx.parking.vo.ParkingReserveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking/reserve")
@Tag(name = "鍋滆溅-车位棰勮")
@Slf4j
public class ParkingReserveController {

    @Autowired
    private ParkingReserveService parkingReserveService;

    @PostMapping
    @Operation(summary = "车位棰勮锛堢敤鎴峰彂璧凤級")
    public Result<Map<String, Object>> createReserve(@RequestBody ParkingReserveCreateDTO dto) {
        try {
            Long reserveId = parkingReserveService.createReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("reserveId", reserveId);
            data.put("expireTime", dto.getReserveEndTime());
            data.put("qrCode", "https://example.com/qr/" + reserveId);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建车位棰勮异常", e);
            return Result.fail("棰勮失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "鍙栨秷棰勮锛堢敤鎴蜂富鍔ㄥ彇娑堬級")
    public Result<Map<String, Object>> cancelReserve(@RequestBody ParkingReserveCancelDTO dto) {
        try {
            Boolean success = parkingReserveService.cancelReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            return success ? Result.success(data) : Result.fail("鍙栨秷失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("鍙栨秷车位棰勮异常", e);
            return Result.fail("鍙栨秷失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/my/list")
    @Operation(summary = "鎴戠殑棰勮鍒楄〃锛堢敤鎴锋煡鐪嬶級")
    public Result<Map<String, Object>> listMyReserves(@RequestParam("userId") Long userId,
                                                      @RequestParam(value = "status", required = false) String status,
                                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            ParkingReserveQueryDTO dto = new ParkingReserveQueryDTO();
            dto.setStatus(status);
            dto.setPageNum(pageNum);
            dto.setPageSize(pageSize);
            IPage<ParkingReserveVO> page = parkingReserveService.listMyReserves(userId, dto);

            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询鎴戠殑棰勮鍒楄〃异常", e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/admin/list")
    @Operation(summary = "管理员樻煡鐪嬮璁㈠垪琛?)
    public Result<Map<String, Object>> adminListReserves(@RequestParam(value = "spaceNo", required = false) String spaceNo,
                                                         @RequestParam(value = "userId", required = false) Long userId,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            ParkingReserveQueryDTO dto = new ParkingReserveQueryDTO();
            dto.setSpaceNo(spaceNo);
            dto.setUserId(userId);
            dto.setStatus(status);
            dto.setPageNum(pageNum);
            dto.setPageSize(pageSize);

            IPage<ParkingReserveVO> page = parkingReserveService.adminListReserves(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员樻煡璇㈤璁㈠垪琛ㄥ紓甯?, e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @PostMapping("/admin/cancel")
    @Operation(summary = "管理员樺己鍒跺彇娑堥璁?)
    public Result<Map<String, Object>> adminCancelReserve(@RequestBody ParkingReserveAdminCancelDTO dto) {
        try {
            Boolean success = parkingReserveService.adminCancelReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            return success ? Result.success(data) : Result.fail("鍙栨秷失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员樺己鍒跺彇娑堥璁㈠紓甯?, e);
            return Result.fail("鍙栨秷失败锛岃绋嶅悗鍐嶈瘯");
        }
    }
}


