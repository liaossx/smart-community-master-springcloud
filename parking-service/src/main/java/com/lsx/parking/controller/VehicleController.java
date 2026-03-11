package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.parking.dto.VehicleBindDTO;
import com.lsx.parking.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.parking.dto.ParkingCarAuditDTO;
import com.lsx.parking.vo.ParkingCarAuditVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/vehicle")
@Tag(name = "车辆管理员")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @PostMapping("/bind")
    @Operation(summary = "绑定车辆")
    public Result<Void> bindVehicle(@RequestBody VehicleBindDTO dto) {
        Long userId = dto.getUserId();
        // 濡傛灉 DTO 涓病浼?userId锛屽皾璇曚粠 UserContext 鑾峰彇锛堥€傞厤业主绔洿鎺ヨ皟鐢級
        if (userId == null) {
            userId = UserContext.getCurrentUserId();
        }
        if (userId == null) {
            return Result.fail("鏈櫥褰曟垨参数错误");
        }
        dto.setUserId(userId);
        
        try {
            vehicleService.bindVehicle(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/audit/list")
    @Operation(summary = "查询车辆瀹℃牳鍒楄〃")
    public Result<IPage<ParkingCarAuditVO>> listAudit(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        IPage<ParkingCarAuditVO> page = vehicleService.listAudit(status, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/audit")
    @Operation(summary = "瀹℃牳车辆绑定申请")
    public Result<Void> auditCar(@RequestBody ParkingCarAuditDTO dto) {
        try {
            vehicleService.auditCar(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
