package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.parking.dto.BindPlateDTO;
import com.lsx.parking.service.ParkingPlateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking/plate")
@RequiredArgsConstructor
@Tag(name = "车牌绑定接口")
public class ParkingPlateController {

    private final ParkingPlateService plateService;

    @Operation(summary = "绑定车牌鍒拌溅浣?)
    @PostMapping("/bind")

    public Result<Void> bindPlate(@RequestBody BindPlateDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        plateService.bindPlate(userId, dto.getSpaceId(), dto.getPlateNo());
        return Result.success();
    }

    @Operation(summary = "瑙ｇ粦车牌")
    @PostMapping("/unbind")
    public Result<Void> unbindPlate(@RequestBody BindPlateDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        plateService.unbindPlate(userId, dto.getSpaceId(), dto.getPlateNo());
        return Result.success();
    }

    @Operation(summary = "查询车位绑定鐨勮溅鐗?)
    @GetMapping("/list")
    public Result<List<String>> listPlates(@RequestParam Long spaceId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(plateService.getPlatesBySpace(userId, spaceId));
    }
}
