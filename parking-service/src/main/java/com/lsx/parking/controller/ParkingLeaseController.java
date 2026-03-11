package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.parking.service.ParkingLeaseService;
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
@RequestMapping("/api/parking/lease")
@Tag(name = "鍋滆溅-鏈堝崱")
public class ParkingLeaseController {

    @Autowired
    private ParkingLeaseService parkingLeaseService;

    /**
     * 璐拱/缁垂鏈堝崱锛岀敓鎴愯鍗?     */
    @PostMapping("/order/create")
    @Operation(summary = "创建鏈堝崱订单", description = "璐拱鎴栫画璐规湀鍗★紝生成鏈敮浠樿鍗?)
    public Result<Long> createLeaseOrder(@RequestBody ParkingLeaseOrderCreateDTO dto) {
        try {
            Long orderId = parkingLeaseService.createLeaseOrder(dto);
            return Result.success(orderId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建鏈堝崱订单异常", e);
            return Result.fail("创建鏈堝崱订单失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    /**
     * 鏀粯鏈堝崱订单
     */
    @PostMapping("/order/pay")
    @Operation(summary = "鏀粯鏈堝崱订单", description = "鏀粯鏈堝崱订单鍚庣郴统计嚜鍔ㄧ敓鏁堟垨缁湡")
    public Result<Void> payLeaseOrder(@RequestBody ParkingLeaseOrderPayDTO dto) {
        try {
            log.info("鎺ユ敹鍒版湀鍗℃敮浠樿姹? 订单ID={}, 鏀粯娓犻亾={}", dto.getOrderId(), dto.getPayChannel());
            parkingLeaseService.payLeaseOrder(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("鏀粯鏈堝崱订单异常", e);
            return Result.fail("鏀粯鏈堝崱订单失败锛岃绋嶅悗鍐嶈瘯");
        }
    }
}

