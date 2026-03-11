package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingSpaceQueryDTO {

    @Schema(description = "椤电爜", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "椤靛ぇ灏?, defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "车位缂栧彿")
    private String spaceNo;

    @Schema(description = "车位状态€?(AVAILABLE/DISABLED)")
    private String status;

    @Schema(description = "车牌鍙?)
    private String plateNo;

    @Schema(description = "社区ID锛堢鐞嗗憳绔寜社区闅旂锛?)
    private Long communityId;
}

