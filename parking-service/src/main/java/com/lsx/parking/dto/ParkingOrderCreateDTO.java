package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ParkingOrderCreateDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "车位ID锛堝浐瀹氳溅浣嶈鍗曢渶浼狅紝浼犱簡浼氳嚜鍔ㄦ牴鎹溅浣嶇被鍨嬭缃鍗曠被鍨嬶級")
    private Long spaceId;

    @Schema(description = "订单绫诲瀷 TEMP/FIXED锛堜紶浜唖paceId鏃朵細鑷姩浠庤溅浣嶈幏鍙栵紝临时订单鍙笉浼犻粯璁や负TEMP锛?)
    private String orderType;

    @Schema(description = "订单金额", required = true)
    private BigDecimal amount;

    @Schema(description = "棰勮开始€濮嬫椂闂?)
    private LocalDateTime startTime;

    @Schema(description = "棰勮结束时间")
    private LocalDateTime endTime;

    @Schema(description = "车牌鍙?备注")
    private String remark;
}




