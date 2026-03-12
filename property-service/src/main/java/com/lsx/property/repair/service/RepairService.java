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
    // 提交报修
    boolean submitRepair(RepairDto repairdto);

    // 更新报修状态
    boolean updateRepairStatus(Long repairId, String status, String remark);

    // 查询我的报修
    IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize);
    
    // 查询所有报修
    IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword);
    
    // 查询指定用户报修
    IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize);
    
    // 批量更新状态
    boolean batchUpdateStatus(List<Long> repairIds, String status, String remark);
    
    // 导出报修记录
    void exportRepairs(String status, String keyword, HttpServletResponse response);
    
    // 获取报修统计
    RepairStatsResult getRepairStats();
}
