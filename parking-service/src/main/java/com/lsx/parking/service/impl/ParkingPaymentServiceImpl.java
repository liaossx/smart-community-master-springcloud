package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.dto.ParkingPaySuccessDTO;
import com.lsx.parking.entity.ParkingGateLog;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingGateLogMapper;
import com.lsx.parking.mapper.ParkingOrderMapper;
import com.lsx.parking.service.ParkingPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ParkingPaymentServiceImpl implements ParkingPaymentService {

    @Autowired
    private ParkingOrderMapper parkingOrderMapper;

    @Autowired
    private ParkingGateLogMapper gateLogMapper;

    @Override
    @Transactional
    public void paySuccess(ParkingPaySuccessDTO dto) {

        // 1锔忊儯 查询鍗?        ParkingOrder order = parkingOrderMapper.selectOne(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getOrderNo, dto.getOrderNo())
                        .last("LIMIT 1")
        );

        if (order == null) {
            throw new RuntimeException("订单不存在?);
        }

        if ("PAID".equals(order.getStatus())) {
            return; // 骞傜瓑锛岄槻姝㈤噸澶嶅洖璋?        }

        // 2锔忊儯 更新订单状态€?        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(dto.getPayChannel());
        order.setUpdateTime(LocalDateTime.now());
        parkingOrderMapper.updateById(order);

        // 3锔忊儯 鍐欏嚭闂告棩蹇楋紙鐪熸鏀捐锛?        ParkingGateLog exitLog = new ParkingGateLog();
        exitLog.setPlateNo(order.getPlateNo());
        exitLog.setUserId(order.getUserId());
        exitLog.setSpaceId(order.getSpaceId());
        exitLog.setGateType(order.getOrderType());
        exitLog.setAction("EXIT");
        exitLog.setResult("SUCCESS");
        exitLog.setRemark("鏀粯瀹屾垚锛岃嚜鍔ㄦ斁琛?);
        exitLog.setCreateTime(LocalDateTime.now());

        gateLogMapper.insert(exitLog);
    }
}
