package com.lsx.property.repair.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairResult {
    private Long id;              // 报修ID
    private String communityName; // 社区名称
    private String buildingNo;    // 楼栋号
    private String houseNo;       // 房屋号

    // 故障信息
    private String faultType;     // 故障类型
    private String faultDesc;     // 故障描述
    private List<String> faultImgs; // 图片列表

    // 状态信息
    private String status;        // 状态 pending/processing/completed/cancelled
    private String statusDesc;    // 状态描述
    private String handleRemark;  // 处理备注

    // 时间信息
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
