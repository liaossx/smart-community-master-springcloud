package com.lsx.property.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_repair")
public class Repair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;         // 用户ID
    private Long communityId;    // 社区ID
    private Long houseId;        // 房屋ID
    private String faultType;    // 故障类型
    private String faultDesc;    // 故障描述
    private String faultImgs;    // 故障图片
    private String status;       // 状态 pending/processing/completed/cancelled
    private String handleRemark; // 处理备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
