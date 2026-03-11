package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CurrentFeeDTO {
    private Long feeId;              // 鐠愶箑宕烮D
    private String communityName;    // 鐏忓繐灏崥宥囆?    private String buildingNo;       // 濡ゅ吋鐖ч崣?    private String houseNo;          // 閹村灝鐪跨紓鏍у娇
    private BigDecimal area;         // 閹村灝鐪块棃銏⑿?    private String feeCycle;         // 閺€鎯板瀭閸涖劍婀?    private BigDecimal feeAmount;    // 鐠愶箑宕熼柌鎴︻杺
    private LocalDateTime dueDate;   // 閹搭亝顒涢弮銉︽埂
}
