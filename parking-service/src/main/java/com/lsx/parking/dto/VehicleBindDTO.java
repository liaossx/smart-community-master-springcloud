package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VehicleBindDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "车牌鍙?, required = true, example = "绮88888")
    private String plateNo;

    @Schema(description = "车辆品牌", example = "濂旈┌")
    private String brand;

    @Schema(description = "车辆颜色", example = "榛戣壊")
    private String color;

    @Schema(description = "申请绑定鐨勮溅浣岻D", required = true)
    private Long spaceId;
}
