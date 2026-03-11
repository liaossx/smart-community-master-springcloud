package com.lsx.parking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.entity.ParkingSpaceLease;

public interface ParkingLeaseService {
    Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto);
    void payLeaseOrder(ParkingLeaseOrderPayDTO dto); // 鏀规垚 DTO
}
