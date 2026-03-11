package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingGateExitDTO {

    @Schema(description = "车牌鍙?, required = true)
    private String plateNo;
}
