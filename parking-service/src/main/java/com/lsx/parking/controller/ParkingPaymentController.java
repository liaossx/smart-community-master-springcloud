package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingPaySuccessDTO;
import com.lsx.parking.service.ParkingPaymentService;
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
@RequestMapping("/api/parking/pay")
@Tag(name = "鍋滆溅-鏀粯")
public class ParkingPaymentController {

    @Autowired
    private ParkingPaymentService parkingPaymentService;

    @PostMapping("/success")
    @Operation(summary = "鏀粯成功鍥炶皟锛堟ā鎷燂級")
    public Result<Void> paySuccess(@RequestBody ParkingPaySuccessDTO dto) {
        try {
            parkingPaymentService.paySuccess(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("鏀粯鍥炶皟异常", e);
            return Result.fail("鏀粯澶勭悊失败");
        }
    }
}
