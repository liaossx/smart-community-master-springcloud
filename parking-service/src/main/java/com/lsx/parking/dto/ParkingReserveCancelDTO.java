package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingReserveCancelDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "棰勮ID")
    private Long reserveId;
}


