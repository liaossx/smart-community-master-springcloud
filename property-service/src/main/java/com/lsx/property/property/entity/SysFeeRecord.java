package com.lsx.property.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee_record")
public class SysFeeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 娑撳鏁璉D
    private Long feeId;              // 閸忓疇浠堢拹锕€宕烮D
    private Long userId;             // 缂傜鍨傛禍绡扗
    private Long houseId;            // 閹村灝鐪縄D
    private BigDecimal payAmount;    // 鐎圭偤妾紓纾嬪瀭闁叉垿顤?    private String payType;          // 閺€顖欑帛閺傜懓绱￠敍鍦礒CHAT/ALIPAY/CASH销毁?    private LocalDateTime payTime;   // 缂傜鍨傞弮鍫曟？
    private String orderNo;          // 閺€顖欑帛鐠併垹宕熼崣?    private String tradeNo;          // 閺€顖欑帛娴溿倖妲楅崣?    private String status = "FAIL";  // 缂傜鍨傞悩鑸碘偓渚婄礄SUCCESS/FAIL/REFUND销毁?    private String remark;           // 婢跺洦鏁?}
