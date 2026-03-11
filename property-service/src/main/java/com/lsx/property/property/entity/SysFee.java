package com.lsx.property.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee") // MyBatis-Plus 濞夈劏袙销毁涘瓰PA 閻?@Entity
public class SysFee {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 娑撳鏁璉D
    private Long houseId;            // 閹村灝鐪縄D
    private Long communityId;        // 鐏忓繐灏疘D销毁涘牆褰叉稉绨剈ll销毁?    private String buildingNo;       // 濡ゅ吋鐖ч崣?    private String feeCycle;         // 閺€鎯板瀭閸涖劍婀￠敍鍫濐洤2025-01销毁?    private BigDecimal feeAmount;    // 鐠愶箑宕熼柌鎴︻杺
    private String feeType = "閻椻晙绗熺拹?; // 鐠愬湱鏁ょ猾璇茬€?    private String status = "UNPAID";// 閻樿埖鈧緤绱橴NPAID/PAID/OVERDUE销毁?    
    private Integer remindCount = 0; // 閸岊剛鍗冲▎鈩冩殶

    private LocalDateTime dueDate;  // 瑜拌绨抽弨閫涜礋 String

    private LocalDateTime createTime;// 閸掓稑缂撻弮鍫曟？
    private LocalDateTime updateTime;// 閺囧瓨鏌婇弮鍫曟？
    private String remark;           // 婢跺洦鏁?}
