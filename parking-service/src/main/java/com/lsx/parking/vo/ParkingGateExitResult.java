package com.lsx.parking.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParkingGateExitResult {

    /** 鏄惁鍏佽鐩存帴鏀捐 */
    private boolean allowPass;

    /** 鏄惁闇€瑕佹敮浠?*/
    private boolean needPay;

    /** 鍋滆溅璐圭敤锛坣eedPay=true 鏃跺繀鏈夛級 */
    private BigDecimal amount;

    /** 订单鍙凤紙needPay=true 鏃跺繀鏈夛級 */
    private String orderNo;

    /** 提示淇℃伅 */
    private String message;
}
