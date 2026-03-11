package com.lsx.property.stats.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.complaint.entity.SysComplaint;
import com.lsx.property.complaint.service.ComplaintService;
import com.lsx.property.repair.entity.Repair;
import com.lsx.property.repair.service.RepairService;
import com.lsx.property.repair.vo.RepairStatsResult;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.service.VisitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PropertyStatsController {

    @Resource
    private ComplaintService complaintService;

    @Resource
    private RepairService repairService;

    @Resource
    private VisitorService visitorService;

    // 投诉统计
    @GetMapping("/complaint/stats")
    public Map<String, Object> getComplaintStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Map<String, Object> complaintStats = new HashMap<>();
        LambdaQueryWrapper<SysComplaint> complaintWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            complaintWrapper.eq(SysComplaint::getCommunityId, communityId);
        }
        // 总数
        complaintStats.put("total", complaintService.count(complaintWrapper));
        // 待处理
        complaintStats.put("pending", complaintService.count(complaintWrapper.clone().eq(SysComplaint::getStatus, "PENDING")));
        // 已处理 (非PENDING)
        complaintStats.put("processed", complaintService.count(complaintWrapper.clone().ne(SysComplaint::getStatus, "PENDING")));
        return complaintStats;
    }

    @GetMapping("/complaint/stats/type")
    public List<Map<String, Object>> getComplaintTypeStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        QueryWrapper<SysComplaint> typeWrapper = new QueryWrapper<>();
        typeWrapper.select("type as name, count(*) as value").groupBy("type");
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            typeWrapper.eq("community_id", communityId);
        }
        return complaintService.listMaps(typeWrapper);
    }

    // 报修统计
    @GetMapping("/repair/stats")
    public Map<String, Object> getRepairStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        // RepairService 内部已经处理了权限过滤，直接调用即可
        RepairStatsResult repairResult = repairService.getRepairStats();
        Map<String, Object> repairStats = new HashMap<>();
        if (repairResult != null) {
            repairStats.put("total", repairResult.getTotal());
            repairStats.put("pending", repairResult.getPending());
            repairStats.put("processing", repairResult.getProcessing());
            repairStats.put("completed", repairResult.getCompleted());
            repairStats.put("cancelled", repairResult.getCancelled());
        } else {
            repairStats.put("total", 0);
            repairStats.put("pending", 0);
        }
        
        // 新增：统计今日报修数 (需要手动加权限过滤)
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<Repair> repairTodayWrapper = new LambdaQueryWrapper<>();
        repairTodayWrapper.ge(Repair::getCreateTime, todayStart).le(Repair::getCreateTime, todayEnd);
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            repairTodayWrapper.eq(Repair::getCommunityId, communityId);
        }
        long todayCount = repairService.count(repairTodayWrapper);
        repairStats.put("today", todayCount);
        
        return repairStats;
    }
    
    @GetMapping("/repair/stats/trend")
    public List<Map<String, Object>> getRepairTrend() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        List<Map<String, Object>> repairTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
            
            LambdaQueryWrapper<Repair> trendWrapper = new LambdaQueryWrapper<>();
            trendWrapper.ge(Repair::getCreateTime, startOfDay)
                        .le(Repair::getCreateTime, endOfDay);
            
            if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
                trendWrapper.eq(Repair::getCommunityId, communityId);
            }
            
            long count = repairService.count(trendWrapper);
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.format(dateFmt));
            dayStat.put("count", count);
            repairTrend.add(dayStat);
        }
        return repairTrend;
    }

    // 访客统计
    @GetMapping("/visitor/stats")
    public Map<String, Object> getVisitorStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Map<String, Object> visitorStats = new HashMap<>();
        LambdaQueryWrapper<SysVisitor> visitorWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            visitorWrapper.eq(SysVisitor::getCommunityId, communityId);
        }
        visitorStats.put("total", visitorService.count(visitorWrapper));
        visitorStats.put("pending", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "PENDING")));
        visitorStats.put("approved", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "APPROVED")));
        visitorStats.put("rejected", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "REJECTED")));
        return visitorStats;
    }
}
