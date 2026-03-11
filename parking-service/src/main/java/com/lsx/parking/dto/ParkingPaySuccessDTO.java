package com.lsx.parking.dto;

import lombok.Data;

@Data
public class ParkingPaySuccessDTO {

    /** 订单鍙?*/
    private String orderNo;

    /** 鏀粯娓犻亾 */
    private String payChannel; // WECHAT / ALIPAY
}
