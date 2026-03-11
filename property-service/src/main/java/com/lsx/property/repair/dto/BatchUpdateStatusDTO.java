package com.lsx.property.repair.dto;

import lombok.Data;

import java.util.List;

/**
 * 閹靛綊鍣洪弴瀛樻煀閹躲儰鎱ㄩ悩鑸碘偓涓廡O
 */
@Data
public class BatchUpdateStatusDTO {
    private List<Long> repairIds;  // 閹躲儰鎱↖D閸掓銆?    private String status;         // 閻╊喗鐖ｉ悩鑸碘偓?    private String remark;         // 婢跺洦鏁為敍鍫濆讲闁绱?}

