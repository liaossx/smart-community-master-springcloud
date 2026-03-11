package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeHistoryDTO {
    private Long recordId;           // 缂傜鍨傜拋鏉跨秿ID
    private String feeCycle;         // 鐎电懓绨茬拹锕€宕熼崨銊︽埂
    private String feeType;          // 鐠愬湱鏁ょ猾璇茬€?    private BigDecimal payAmount;    // 缂傜鍨傞柌鎴︻杺
    private String payType;          // 閺€顖欑帛閺傜懓绱?    private LocalDateTime payTime;   // 缂傜鍨傞弮鍫曟？
    private String status;           // 缂傜鍨傞悩鑸碘偓?}
