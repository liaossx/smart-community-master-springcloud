package com.lsx.property.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.property.repair.dto.RepairDto;
import com.lsx.property.repair.entity.Repair;
import com.lsx.property.repair.vo.RepairResult;
import com.lsx.property.repair.vo.RepairStatsResult;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

public interface RepairService extends IService<Repair> {
    // 閹绘劒姘﹂幎銉ゆ叏销毁涘牅绗熸稉鑽ゆ暏销毁?    boolean submitRepair(RepairDto repairdto);

    // 閺囧瓨鏌婇幎銉ゆ叏閻樿埖鈧緤绱欑粻锛勬倞閸涙鏁ら敍?    boolean updateRepairStatus(Long repairId, String status, String remark);

    //娑撴矮瀵岄弻銉嚄閼奉亜绻侀惃鍕Г娣囶喛顔囪ぐ?    IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize);
    //缁狅紕鎮婇崨妯荤叀鐠囥垺澧嶉張澶屾畱閹躲儰鎱ㄧ拋鏉跨秿
    IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword);
    //閺屻儴顕楅崡鏇氶嚋娑撴矮瀵岄惃鍕Г娣囶喛顔囪ぐ?    IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize);
    
    // 閹靛綊鍣洪弴瀛樻煀閹躲儰鎱ㄩ悩鑸碘偓?    boolean batchUpdateStatus(List<Long> repairIds, String status, String remark);
    
    // 鐎电厧鍤幎銉ゆ叏閺佺増宓?    void exportRepairs(String status, String keyword, HttpServletResponse response);
    
    // 閼惧嘲褰囬幎銉ゆ叏缂佺喕顓搁弫鐗堝祦
    RepairStatsResult getRepairStats();

}
