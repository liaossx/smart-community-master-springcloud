package com.lsx.parking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingReserveVO {

    @Schema(description = "棰勮ID")
    private Long reserveId;

    @Schema(description = "车位ID")
    private Long spaceId;

    @Schema(description = "车位缂栧彿")
    private String spaceNo;

    @Schema(description = "灏忓尯鍚嶇О")
    private String communityName;

    @Schema(description = "预约状态€?)
    private String status;

    @Schema(description = "状态€佹枃鏈?)
    private String statusText;

    @Schema(description = "预约开始€濮嬫椂闂?)
    private LocalDateTime reserveStartTime;

    @Schema(description = "预约结束时间")
    private LocalDateTime reserveEndTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "用户ID锛堢鐞嗗憳鍒楄〃鐢級")
    private Long userId;

    @Schema(description = "用户濮撳悕锛堢鐞嗗憳鍒楄〃鐢級")
    private String userName;

    @Schema(description = "所有嬫満鍙凤紙管理员樺垪琛ㄧ敤锛?)
    private String phone;
}


