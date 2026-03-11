package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingReserveQueryDTO {

    @Schema(description = "椤电爜", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "椤靛ぇ灏?, defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "预约状态€?RESERVED/EXPIRED/CANCELLED")
    private String status;

    @Schema(description = "车位缂栧彿锛堢鐞嗗憳查询鐢級")
    private String spaceNo;

    @Schema(description = "用户ID锛堢鐞嗗憳查询鐢級")
    private Long userId;
}


