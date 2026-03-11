package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpaceOpenDTO {

    @Schema(description = "车位ID鎴栫粦瀹氳褰旾D", required = true)
    private Long spaceId;

    @Schema(description = "璐拱时长(鏈?", required = true)
    private Integer durationMonths;

    @Schema(description = "鏀粯方式 (濡?BALANCE)", required = true)
    private String payMethod;

    @Schema(description = "鏀粯金额", required = true)
    private BigDecimal amount;

    @Schema(description = "用户ID (鍙€夛紝鑻ヤ笉浼犲垯浠嶤ontext鑾峰彇)")
    private Long userId;
}

