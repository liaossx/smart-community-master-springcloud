package com.lsx.system.admin.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.system.client.HouseServiceClient;
import com.lsx.system.client.PropertyServiceClient;
import com.lsx.system.client.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@Tag(name = "管理员-数据统计接口")
public class AdminStatsController {

    @Resource
    private PropertyServiceClient propertyServiceClient;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private HouseServiceClient houseServiceClient;

    @GetMapping("/overview")
    @Operation(summary = "获取首页统计概览")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();

        // 0. 社区统计
        Map<String, Object> communityStats = new HashMap<>();
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
            communityStats.put("total", houseServiceClient.countCommunities());
        } else {
            communityStats.put("total", communityId != null ? 1 : 0);
        }
        data.put("community", communityStats);

        // 1. 投诉统计
        data.put("complaint", propertyServiceClient.getComplaintStats());

        // 2. 报修统计
        data.put("repair", propertyServiceClient.getRepairStats());

        // 3. 访客统计
        data.put("visitor", propertyServiceClient.getVisitorStats());

        // 4. 用户统计
        Map<String, Object> userStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            userStats.put("total", userServiceClient.countUsers());
            userStats.put("owner", userServiceClient.countUsersByRole("owner"));
            userStats.put("admin", userServiceClient.countUsersByRole("admin"));
        } else {
            // 普通管理员：只统计本社区已绑定房屋的业主（去重）
            Long ownerCount = 0L;
            if (communityId != null) {
                ownerCount = houseServiceClient.countOwnersByCommunityId(communityId);
            }
            userStats.put("total", ownerCount);
            userStats.put("owner", ownerCount);
            userStats.put("admin", 0);
        }
        data.put("user", userStats);

        // 5. [图表] 投诉类型分布
        data.put("complaintType", propertyServiceClient.getComplaintTypeStats());

        // 6. [图表] 近7日报修趋势
        data.put("repairTrend", propertyServiceClient.getRepairTrend());

        return Result.success(data);
    }
}
