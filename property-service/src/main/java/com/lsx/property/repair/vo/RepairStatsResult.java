package com.lsx.property.repair.vo;

import lombok.Data;

/**
 * 閹躲儰鎱ㄧ紒鐔活吀缂佹挻鐏? */
@Data
public class RepairStatsResult {
    private Integer total;      // 閹粯濮ゆ穱顔芥殶
    private Integer pending;    // 瀵板懎顦╅悶鍡樻殶
    private Integer processing; // 婢跺嫮鎮婃稉顓熸殶
    private Integer completed;  // 瀹告彃鐣幋鎰殶
    private Integer cancelled;  // 瀹告彃褰囧☉鍫熸殶
}

