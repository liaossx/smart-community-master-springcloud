package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GenerateFeeDTO {
    private String communityName;    // 鐏忓繐灏崥宥囆為敍鍫滆礋缁屽搫鍨崗銊ョ毈閸栫尨绱?    private String buildingNo;       // 濡ゅ吋鐖ч崣鍑ょ礄娑撹櫣鈹栭崚娆忓弿濡ゅ吋鐖ч敍?    private String feeCycle;         // 閺€鎯板瀭閸涖劍婀￠敍鍫濐洤2025-01销毁?    private String dueDate;   // 閹搭亝顒涢弮銉︽埂
    private BigDecimal unitPrice;    // 閸楁洑鐜敍鍫濆帗/閵曗槄绱濈粻锛勬倞閸涙绶崗銉礆
}
