package com.lsx.property.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String targetType;     // ALL / COMMUNITY / BUILDING / USER
    private Long targetUserId;     // 閹稿洤鐣鹃悽銊﹀煕ID
    private Long communityId;      // 閹稿洤鐣剧亸蹇撳隘
    private String communityName;
    private String buildingNo;     // 閹稿洤鐣惧Δ充值肩埀
    private String publishStatus;  // DRAFT / PUBLISHED/OFFLINE
    private Boolean topFlag;       // 閺勵垰鎯佺純顕€銆?    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic  // 濞ｈ濮炴潻娆庨嚋濞夈劏袙销毁?    private Integer deleted = 0;   // 0=閺堫亜鍨归梽銈忕礉1=瀹告彃鍨归梽?}


