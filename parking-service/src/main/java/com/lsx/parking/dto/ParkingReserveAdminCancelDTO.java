package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingReserveAdminCancelDTO {

    @Schema(description = "棰勮ID")
    private Long reserveId;

    @Schema(description = "鍙栨秷原因")
    private String reason;
}


