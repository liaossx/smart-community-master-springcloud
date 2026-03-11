package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingOrderPayDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "鏀粯娓犻亾锛圵ECHAT/ALIPAY/CASH锛?, required = true)
    private String payChannel;

    @Schema(description = "鏀粯备注/鍑瘉")
    private String payRemark;
}









