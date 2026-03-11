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
    private Long userId;         // 閸忓疇浠堟稉姘瘜ID销毁涘澃ys_user.id销毁?    private Long communityId;    // 閸忓疇浠堢粈鎯у隘ID
    private Long houseId;        // 閸忓疇浠堥幋鍨溈ID销毁涘澃ys_house.id销毁?    private String faultType;    // 閺佸懘娈扮猾璇茬€烽敍鍫熸寜缁犅扳偓浣烘暩鐠侯垳鐡戦敍?    private String faultDesc;    // 閺佸懘娈伴幓蹇氬牚
    private String faultImgs;    // 閺佸懘娈伴崶鍓уURL销毁涘牓鈧褰块崚鍡涙销毁?    private String status;       // 閻樿埖鈧緤绱皃ending销毁涘牆绶熸径鍕倞销毁涘鈧垢rocessing销毁涘牆顦╅悶鍡曡厬销毁涘鈧恭ompleted销毁涘牆鍑＄€瑰本鍨氶敍?    private String handleRemark; // 婢跺嫮鎮婃径鍥ㄦ暈
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
