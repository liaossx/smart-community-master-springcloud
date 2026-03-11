package com.lsx.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingOrderCreateDTO;
import com.lsx.parking.dto.ParkingOrderPayDTO;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.service.ParkingOrderService;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingOrderVO;
import com.lsx.parking.client.UserServiceClient;
import com.lsx.parking.dto.external.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking/order")
@Tag(name = "停车订单接口")
@Slf4j
public class ParkingOrderController {

    @Autowired
    private ParkingOrderService parkingOrderService;

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private UserServiceClient userServiceClient;

    @PostMapping
    @Operation(summary = "生成停车订单", description = "支持临时停车与固定车位缴费")
    public Result<Long> createOrder(@RequestBody ParkingOrderCreateDTO dto) {
        try {
            Long orderId = parkingOrderService.createOrder(dto);
            return Result.success(orderId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建鍋滆溅订单异常", e);
            return Result.fail("创建失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/admin/list")
    @Operation(summary = "管理员樺垎椤垫煡璇㈠仠杞﹁鍗?, description = "鏀寔鎸夎溅鐗屽彿銆佺姸鎬併€佽鍗曠被鍨嬨€佹椂闂磋寖鍥寸瓫閫?)
    public Result<Map<String, Object>> adminListOrders(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(value = "plateNo", required = false) String plateNo,
                                                       @RequestParam(value = "status", required = false) String status,
                                                       @RequestParam(value = "orderType", required = false) String orderType,
                                                       @RequestParam(value = "startDate", required = false) String startDate,
                                                       @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            IPage<ParkingOrder> page = parkingOrderService.adminListOrders(pageNum, pageSize, plateNo, status, orderType, startDate, endDate);
            List<Map<String, Object>> records = new ArrayList<>();
            for (ParkingOrder order : page.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("orderId", order.getId());
                item.put("orderNo", order.getOrderNo());
                item.put("plateNo", order.getPlateNo());

                String spaceNo = null;
                if (order.getSpaceId() != null) {
                    ParkingSpace space = parkingSpaceService.getById(order.getSpaceId());
                    if (space != null) {
                        spaceNo = space.getSpaceNo();
                    }
                }
                item.put("spaceNo", spaceNo);

                String ownerName = null;
                if (order.getUserId() != null) {
                    UserDTO user = userServiceClient.getUserById(order.getUserId());
                    if (user != null) {
                        ownerName = user.getRealName();
                    }
                }
                item.put("ownerName", ownerName);

                item.put("amount", order.getAmount());
                item.put("status", order.getStatus());
                item.put("orderType", order.getOrderType());
                item.put("startTime", order.getStartTime());
                item.put("endTime", order.getEndTime());
                item.put("payTime", order.getPayTime());
                item.put("payChannel", order.getPayChannel());

                records.add(item);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            Result<Map<String, Object>> result = new Result<>();
            result.setCode(200);
            result.setMsg("操作成功");
            result.setData(data);
            return result;
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员樻煡璇㈠仠杞﹁鍗曞紓甯?, e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @GetMapping("/my")
    @Operation(summary = "查询鎴戠殑鍋滆溅订单", description = "鎸夋椂闂村€掑簭")
    public Result<IPage<ParkingOrderVO>> listMyOrders(@RequestParam("userId") Long userId,
                                                     @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            IPage<ParkingOrderVO> page = parkingOrderService.listMyOrders(userId, pageNum, pageSize);
            return Result.success(page);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询鍋滆溅订单异常", e);
            return Result.fail("查询失败锛岃绋嶅悗鍐嶈瘯");
        }
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "鏀粯鍋滆溅订单")
    public Result<Boolean> payOrder(@PathVariable("id") Long orderId, @RequestBody ParkingOrderPayDTO dto) {
        try {
            Boolean success = parkingOrderService.payOrder(orderId, dto);
            return success ? Result.success(true) : Result.fail("鏀粯失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("鏀粯订单异常", e);
            return Result.fail("鏀粯失败锛岃绋嶅悗鍐嶈瘯");
        }
    }
}



