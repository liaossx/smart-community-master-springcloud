package com.lsx.core.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.community.entity.Community;
import com.lsx.core.community.service.CommunityService;
import com.lsx.core.complaint.entity.SysComplaint;
import com.lsx.core.complaint.service.ComplaintService;
import com.lsx.core.repair.entity.Repair;
import com.lsx.core.repair.service.RepairService;
import com.lsx.core.repair.vo.RepairStatsResult;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.service.UserService;
import com.lsx.core.visitor.entity.SysVisitor;
import com.lsx.core.visitor.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/admin/stats")
@Tag(name = "管理员-数据统计接口")
public class AdminStatsController {

    @Resource
    private ComplaintService complaintService;

    @Resource
    private RepairService repairService;

    @Resource
    private VisitorService visitorService;

    @Resource
    private UserService userService;

    @Resource
    private CommunityService communityService;
    
    @Resource
    private com.lsx.core.house.mapper.UserHouseMapper userHouseMapper;

    @GetMapping("/overview")
    @Operation(summary = "获取首页统计概览")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();

        // 0. 社区统计 (新增)
        Map<String, Object> communityStats = new HashMap<>();
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        // 如果是超级管理员，统计所有小区；如果是普通管理员，统计自己管辖的小区(通常是1个)
        if ("super_admin".equalsIgnoreCase(role)) {
            communityStats.put("total", communityService.count());
        } else {
            communityStats.put("total", communityId != null ? 1 : 0);
        }
        data.put("community", communityStats);

        // 1. 投诉统计
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
        data.put("complaint", complaintStats);

        // 2. 报修统计
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
        
        data.put("repair", repairStats);

        // 3. 访客统计
        Map<String, Object> visitorStats = new HashMap<>();
        LambdaQueryWrapper<SysVisitor> visitorWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            visitorWrapper.eq(SysVisitor::getCommunityId, communityId);
        }
        visitorStats.put("total", visitorService.count(visitorWrapper));
        visitorStats.put("pending", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "PENDING")));
        visitorStats.put("approved", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "APPROVED")));
        visitorStats.put("rejected", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "REJECTED")));
        data.put("visitor", visitorStats);

        // 4. 用户统计
        Map<String, Object> userStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            userStats.put("total", userService.count());
            userStats.put("owner", userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, "owner")));
            userStats.put("admin", userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, "admin")));
        } else {
            // 普通管理员：只统计本社区已绑定房屋的业主（去重）
            Long ownerCount = 0L;
            if (communityId != null) {
                ownerCount = userHouseMapper.countDistinctOwnerByCommunityId(communityId);
            }
            userStats.put("total", ownerCount); // 这里total和owner是一样的，因为管辖范围内都是业主
            userStats.put("owner", ownerCount);
            userStats.put("admin", 0); // 普通管理员看不到其他管理员
        }
        data.put("user", userStats);

        // 5. [图表] 投诉类型分布
        QueryWrapper<SysComplaint> typeWrapper = new QueryWrapper<>();
        typeWrapper.select("type as name, count(*) as value").groupBy("type");
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            typeWrapper.eq("community_id", communityId);
        }
        // 注意：SysComplaint 实体中字段是 type，数据库列名也是 type
        // listMaps 返回的是 List<Map<String, Object>>，正好符合 ECharts 饼图数据格式
        List<Map<String, Object>> complaintTypeStats = complaintService.listMaps(typeWrapper);
        data.put("complaintType", complaintTypeStats);

        // 6. [图表] 近7日报修趋势
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
        data.put("repairTrend", repairTrend);

        return Result.success(data);
    }
}
