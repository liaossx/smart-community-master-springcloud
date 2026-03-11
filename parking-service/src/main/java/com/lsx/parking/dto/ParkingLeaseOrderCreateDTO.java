package com.lsx.parking.dto;

import lombok.Data;

@Data
public class ParkingLeaseOrderCreateDTO {

    private Long userId;
    private Long spaceId;

    /**
     * MONTHLY / YEARLY / PERPETUAL
     */
    private String leaseType;

    /**
     * 车牌鍙?(鍙€夛紝棣栨鍔炵悊鏃剁敤浜庣粦瀹?
     */
    private String plateNo;
}
