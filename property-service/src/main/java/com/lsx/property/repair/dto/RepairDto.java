package com.lsx.property.repair.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RepairDto {
    private Long id;
    private Long userId;         // 用户ID
    private String buildingNo;   // 楼栋号
    private String houseNo;      // 房屋号
    private String faultType;    // 故障类型
    private String faultDesc;    // 故障描述
    private String faultImgs;    // 故障图片
    private String status;       // 状态
    private String handleRemark; // 处理备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
