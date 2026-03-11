package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.dto.ParkingGateOpenDTO;
import com.lsx.parking.service.ParkingGateService;
import com.lsx.parking.vo.ParkingGateExitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/parking/gate")
@Tag(name = "鍋滆溅-开始€闂?)
public class ParkingGateController {

    @Autowired
    private ParkingGateService parkingGateService;


    @PostMapping("/enter")
    @Operation(summary = "车辆鍏ラ椄", description = "所有弿车牌鎴栨壂鐮佸叆闂?)
    public Result<Void> enterGate(@RequestBody ParkingGateEnterDTO dto) {
        try {
            parkingGateService.enterGate(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车辆鍏ラ椄异常", e);
            return Result.fail("鍏ラ椄失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @PostMapping("/exit")
    @Operation(summary = "车辆鍑洪椄")
    public Result<ParkingGateExitResult> exitGate(
            @RequestBody ParkingGateExitDTO dto) {

        ParkingGateExitResult result = parkingGateService.exitGate(dto);
        return Result.success(result);
    }
}

