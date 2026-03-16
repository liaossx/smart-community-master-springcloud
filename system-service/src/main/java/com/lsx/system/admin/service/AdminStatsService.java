package com.lsx.system.admin.service;

import com.lsx.system.client.HouseServiceClient;
import com.lsx.system.client.PropertyServiceClient;
import com.lsx.system.client.UserServiceClient;
import com.lsx.system.client.WorkorderServiceClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminStatsService {

    @Resource
    private PropertyServiceClient propertyServiceClient;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private HouseServiceClient houseServiceClient;

    @Resource
    private WorkorderServiceClient workorderServiceClient;

    @Cacheable(cacheNames = "adminStatsOverview", key = "#role + ':' + ((#role != null && #role.equalsIgnoreCase('super_admin') && #communityIdParam != null) ? #communityIdParam : #tokenCommunityId)")
    public Map<String, Object> getOverviewData(String role, Long tokenCommunityId, Long communityIdParam) {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> communityStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            communityStats.put("total", houseServiceClient.countCommunities());
        } else {
            communityStats.put("total", tokenCommunityId != null ? 1 : 0);
        }
        data.put("community", communityStats);

        data.put("complaint", propertyServiceClient.getComplaintStats());
        data.put("repair", workorderServiceClient.getRepairStats());
        data.put("visitor", propertyServiceClient.getVisitorStats());

        Map<String, Object> userStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            userStats.put("total", userServiceClient.countUsers());
            userStats.put("owner", userServiceClient.countUsersByRole("owner"));
            userStats.put("admin", userServiceClient.countUsersByRole("admin"));
        } else {
            Long ownerCount = 0L;
            if (tokenCommunityId != null) {
                ownerCount = houseServiceClient.countOwnersByCommunityId(tokenCommunityId);
            }
            userStats.put("total", ownerCount);
            userStats.put("owner", ownerCount);
            userStats.put("admin", 0);
        }
        data.put("user", userStats);

        data.put("complaintType", propertyServiceClient.getComplaintTypeStats());
        data.put("repairTrend", workorderServiceClient.getRepairTrend());

        return data;
    }
}
