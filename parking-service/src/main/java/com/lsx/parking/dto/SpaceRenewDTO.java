package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpaceRenewDTO {

    @Schema(description = "车位ID", required = true)
    private Long spaceId;

    @Schema(description = "缁垂鏈堟暟", required = true, example = "1")
    private Integer durationMonths;

    @Schema(description = "鏀粯方式", example = "BALANCE")
    private String payMethod;

    @Schema(description = "鏀粯金额锛堝悗绔渶浜屾鏍￠獙锛?, required = true)
    private BigDecimal amount;

    @Schema(description = "用户ID锛堝彲閫夛紝寤鸿浠嶵oken鍙栵級")
    private Long userId;
}

