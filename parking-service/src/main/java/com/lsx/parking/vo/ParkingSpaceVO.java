package com.lsx.parking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingSpaceVO {

    @Schema(description = "车位ID")
    private Long id;                 // 车位ID

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "车位缂栧彿锛堟棫瀛楁锛屽缓璁敤spaceNo锛?)
    private String slot;             // 车位缂栧彿

    @Schema(description = "车位缂栧彿")
    private String spaceNo;

    @Schema(description = "灏忓尯鍚嶇О")
    private String communityName;    // 灏忓尯鍚嶇О

    @Schema(description = "状态€?(FREE/OCCUPIED)")
    private String status;

    @Schema(description = "业主濮撳悕")
    private String ownerName;

    @Schema(description = "杩囨湡时间")
    private LocalDateTime expireTime;

    /** ===== 租赁淇℃伅锛堟潵鑷?lease 琛級 ===== */
    @Schema(description = "租赁绫诲瀷")
    private String leaseType;        // MONTHLY / YEARLY / PERPETUAL

    @Schema(description = "租赁开始€濮嬫椂闂?)
    private LocalDateTime leaseStartTime;

    @Schema(description = "租赁结束时间")
    private LocalDateTime leaseEndTime;

    @Schema(description = "租赁状态€?)
    private String leaseStatus;      // ACTIVE / EXPIRED

    /** ===== 鍓嶇灞曠ず杈呭姪瀛楁 ===== */
    @Schema(description = "鏄惁鍙敤")
    private Boolean active;           // true = 褰撳墠鍙敤

    @Schema(description = "状态€佹枃鏈?)
    private String statusText;        // 使用中?/ 已过期?
    @Schema(description = "车牌鍙?)
    private String plateNo; //车牌鍙蜂俊鎭?}





