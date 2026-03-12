package com.lsx.property.repair.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.property.client.HouseServiceClient;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.dto.external.HouseDTO;
import com.lsx.property.repair.dto.RepairDto;
import com.lsx.property.repair.entity.Repair;
import com.lsx.property.repair.mapper.RepairMapper;
import com.lsx.property.repair.service.RepairService;
import com.lsx.property.repair.vo.RepairResult;
import com.lsx.property.repair.vo.RepairStatsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {
    
    @Autowired
    private HouseServiceClient houseServiceClient;
    
    @Override
    public boolean submitRepair(RepairDto repairdto) {
        String buildingNo = repairdto.getBuildingNo();
        String houseNo = repairdto.getHouseNo();
        // 1. 校验参数
        if (buildingNo == null || buildingNo.trim().isEmpty()) {
            throw new RuntimeException("楼栋号不能为空");
        }
        if (houseNo == null || houseNo.trim().isEmpty()) {
            throw new RuntimeException("房屋号不能为空");
        }

        // 房屋号格式校验
        String housePattern = "^\\d{3,4}$"; 
        if (!ReUtil.isMatch(housePattern, houseNo.trim())) {
            throw new RuntimeException("房屋号格式不正确，应为3-4位数字，如'101'");
        }

        // 校验房屋是否存在
        HouseDTO house = houseServiceClient.getHouseByInfo(buildingNo, houseNo);
        if (house == null) {
            throw new RuntimeException("房屋信息不存在或未绑定所属社区");
        }
        Long houseId = house.getId();
        
        Repair repair = new Repair();
        BeanUtil.copyProperties(repairdto, repair);
        repair.setHouseId(houseId);
        repair.setStatus("pending");
        if (UserContext.getCommunityId() != null) {
            repair.setCommunityId(UserContext.getCommunityId());
        }
        return baseMapper.insert(repair) > 0;
    }

    @Override
    public boolean updateRepairStatus(Long repairId, String status, String remark) {
        // 1. 查询报修记录
        Repair repair = baseMapper.selectById(repairId);
        if (repair == null) {
            return false;
        }
        // 2. 更新状态
        repair.setStatus(status);
        repair.setHandleRemark(remark);
        return baseMapper.updateById(repair) > 0;
    }

    // 查询我的报修记录
    @Override
    public IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        // 分页查询
        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        // 转换为VO
        return repairPage.convert(this::convertToRepairResult);
    }

    // 查询所有报修记录
    @Override
    public IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<Repair> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 权限控制 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 超级管理员，查询所有
        } else {
             if (currentCommunityId == null) {
                  // 普通管理员未绑定社区，查询为空
                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------

        // 状态筛选
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }

        // 关键字搜索
        if (StringUtils.hasText(keyword)) {
            // 处理搜索关键字
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);

            queryWrapper.and(wrapper -> {
                // 故障描述或类型
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }

                // 房屋ID匹配
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }

        IPage<Repair> repairPage = baseMapper.selectPage(page, queryWrapper);
        return repairPage.convert(this::convertToRepairResult);
    }

    // 处理搜索关键字
    private List<String> processSearchKeyword(String keyword) {
        List<String> searchTerms = new ArrayList<>();
        searchTerms.add(keyword); // 原始关键字

        if (keyword.contains("栋")) {
            String[] parts = keyword.split("栋");
            if (parts.length > 0) {
                String buildingPart = parts[0] + "栋";
                searchTerms.add(buildingPart);
            }

            if (parts.length > 1 && !parts[1].isEmpty()) {
                String housePart = parts[1];
                housePart = housePart.replace("室", "");
                if (!housePart.isEmpty()) {
                    searchTerms.add(housePart);
                }
            }
        }

        return searchTerms.stream().distinct().collect(Collectors.toList());
    }

    // 获取匹配的房屋ID
    private List<Long> getAllMatchingHouseIds(List<String> searchTerms) {
        Set<Long> houseIds = new HashSet<>();

        for (String term : searchTerms) {
            houseIds.addAll(getHouseIdsByKeyword(term));
        }

        return new ArrayList<>(houseIds);
    }

    
    // 调用远程接口搜索房屋ID
    private List<Long> getHouseIdsByKeyword(String keyword) {
        return houseServiceClient.searchHouseIds(keyword);
    }


    /**
     * 查询用户报修记录
     */
    @Override
    public IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        return repairPage.convert(this::convertToRepairResult);
    }


    /**
     * 转换为VO
     */
    private RepairResult convertToRepairResult(Repair repair) {
        if (repair == null) return null;

        RepairResult result = new RepairResult();

        // 1. 基础信息
        result.setId(repair.getId());
        result.setFaultType(repair.getFaultType());
        result.setFaultDesc(repair.getFaultDesc());
        result.setStatus(repair.getStatus());
        result.setHandleRemark(repair.getHandleRemark());
        result.setCreateTime(repair.getCreateTime());
        result.setUpdateTime(repair.getUpdateTime());

        // 2. 图片处理
        if (StringUtils.hasText(repair.getFaultImgs())) {
            List<String> imgs = Arrays.asList(repair.getFaultImgs().split(","));
            result.setFaultImgs(imgs);
        } else {
            result.setFaultImgs(new ArrayList<>());
        }

        // 3. 房屋信息
        if (repair.getHouseId() != null) {
            HouseDTO house = houseServiceClient.getHouseById(repair.getHouseId());
            if (house != null) {
                result.setCommunityName(house.getCommunityName());
                result.setBuildingNo(house.getBuildingNo());
                result.setHouseNo(house.getHouseNo());
            }
        }

        // 4. 状态描述
        result.setStatusDesc(getStatusDesc(repair.getStatus()));

        return result;
    }

    // 获取状态描述
    private String getStatusDesc(String status) {
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return status;
        }
    }

    /**
     * 转换状态描述（冗余方法保留）
     */
    private String convertStatusToDesc(String status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return "未知状态";
        }
    }

    @Override
    public boolean batchUpdateStatus(List<Long> repairIds, String status, String remark) {
        if (repairIds == null || repairIds.isEmpty()) {
            return false;
        }
        
        // 校验状态
        List<String> validStatuses = Arrays.asList("pending", "processing", "completed", "cancelled");
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("状态不合法");
        }
        
        // 批量更新
        List<Repair> repairs = new ArrayList<>();
        for (Long repairId : repairIds) {
            Repair repair = new Repair();
            repair.setId(repairId);
            repair.setStatus(status);
            repair.setHandleRemark(remark);
            repairs.add(repair);
        }
        
        // 批量更新
        return this.updateBatchById(repairs);
    }

    @Override
    public void exportRepairs(String status, String keyword, HttpServletResponse response) {
        // 构建查询条件
        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 权限控制 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 超级管理员
        } else {
             if (currentCommunityId == null) {
                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }
        
        // 关键字筛选
        if (StringUtils.hasText(keyword)) {
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);
            
            queryWrapper.and(wrapper -> {
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }
                
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }
        
        List<Repair> repairs = this.list(queryWrapper);
        
        // 转换为VO
        List<RepairResult> repairResults = repairs.stream()
                .map(this::convertToRepairResult)
                .collect(Collectors.toList());
        
        // 导出CSV
        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=repairs.csv");
            
            try (PrintWriter writer = response.getWriter()) {
                // 写入表头
                writer.println("ID,社区名称,楼栋号,房屋号,故障类型,故障描述,状态,处理备注,创建时间");
                
                // 写入内容
                for (RepairResult result : repairResults) {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            result.getId(),
                            result.getCommunityName(),
                            result.getBuildingNo(),
                            result.getHouseNo(),
                            result.getFaultType(),
                            result.getFaultDesc(),
                            result.getStatusDesc(),
                            result.getHandleRemark() != null ? result.getHandleRemark() : "",
                            result.getCreateTime());
                }
                
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public RepairStatsResult getRepairStats() {
        RepairStatsResult stats = new RepairStatsResult();
        
        // --- 权限控制 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        LambdaQueryWrapper<Repair> baseWrapper = Wrappers.lambdaQuery(Repair.class);
        
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId != null) {
                baseWrapper.eq(Repair::getCommunityId, currentCommunityId);
            } else {
                baseWrapper.eq(Repair::getId, -1L);
            }
        }

        // 统计总数
        stats.setTotal(Math.toIntExact(this.count(baseWrapper)));
        
        // 统计各状态数量
        LambdaQueryWrapper<Repair> pendingQuery = baseWrapper.clone().eq(Repair::getStatus, "pending");
        stats.setPending(Math.toIntExact(this.count(pendingQuery)));
        
        LambdaQueryWrapper<Repair> processingQuery = baseWrapper.clone().eq(Repair::getStatus, "processing");
        stats.setProcessing(Math.toIntExact(this.count(processingQuery)));
        
        LambdaQueryWrapper<Repair> completedQuery = baseWrapper.clone().eq(Repair::getStatus, "completed");
        stats.setCompleted(Math.toIntExact(this.count(completedQuery)));
        
        LambdaQueryWrapper<Repair> cancelledQuery = baseWrapper.clone().eq(Repair::getStatus, "cancelled");
        stats.setCancelled(Math.toIntExact(this.count(cancelledQuery)));
        
        return stats;
    }
}
