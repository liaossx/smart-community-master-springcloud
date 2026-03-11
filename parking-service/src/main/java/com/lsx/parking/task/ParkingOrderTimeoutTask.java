package com.lsx.parking.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingOrderTimeoutTask {

    private final ParkingOrderMapper parkingOrderMapper;

    private static final long TIMEOUT_MINUTES = 15;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelUnpaidOrders() {

        log.info("銆愬畾鏃朵换鍔°€戝紑濮嬫壂鎻忚秴鏃舵湭鏀粯订单");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        // 1锔忊儯 查询鍊欓€夎鍗曪紙鍙煡锛屼笉鐩存帴鏀癸級
        List<ParkingOrder> overdueOrders = parkingOrderMapper.selectList(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getStatus, "UNPAID")
                        .lt(ParkingOrder::getCreateTime, timeoutThreshold)
                        .last("LIMIT 100")
        );

        log.info("銆愬畾鏃朵换鍔°€戝彂鐜?{} 鏉¤秴鏃惰鍗?, overdueOrders.size());

        for (ParkingOrder order : overdueOrders) {
            try {
                // 2锔忊儯 鏉′欢更新锛堟牳蹇冿紒锛?                int rows = parkingOrderMapper.update(
                        null,
                        Wrappers.<ParkingOrder>lambdaUpdate()
                                .eq(ParkingOrder::getId, order.getId())
                                .eq(ParkingOrder::getStatus, "UNPAID")
                                .set(ParkingOrder::getStatus, "CANCEL")
                                .set(ParkingOrder::getUpdateTime, LocalDateTime.now())
                );

                if (rows > 0) {
                    log.info("銆愯鍗曞彇娑堟垚鍔熴€憃rderNo={}", order.getOrderNo());
                } else {
                    log.info("銆愯鍗曞凡琚鐞嗐€憃rderNo={}锛屽彲鑳藉凡鏀粯鎴栧凡鍙栨秷", order.getOrderNo());
                }

            } catch (Exception e) {
                log.error("銆愯鍗曞彇娑堝紓甯搞€憃rderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("銆愬畾鏃朵换鍔°€戞壂鎻忓畬鎴?);
    }
}
