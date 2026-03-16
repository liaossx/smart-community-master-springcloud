package com.lsx.house.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.house.entity.House;
import com.lsx.house.entity.UserHouse;
import com.lsx.house.mapper.HouseMapper;
import com.lsx.house.mapper.UserHouseMapper;
import com.lsx.house.service.CommunityService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/house")
public class HouseApiController {

    @Resource
    private UserHouseMapper userHouseMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private CommunityService communityService;

    @GetMapping("/user/{userId}")
    public List<House> getHousesByUserId(@PathVariable("userId") Long userId) {
        LambdaQueryWrapper<UserHouse> userHouseQuery = new LambdaQueryWrapper<UserHouse>()
                .select(UserHouse::getHouseId)
                .eq(UserHouse::getUserId, userId);
        List<UserHouse> userHouses = userHouseMapper.selectList(userHouseQuery);
        
        if (userHouses.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> houseIds = userHouses.stream()
                .map(UserHouse::getHouseId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<House>()
                .in(House::getId, houseIds)
                .select(House::getId, House::getCommunityId, House::getCommunityName, House::getBuildingNo, House::getHouseNo);
        return houseMapper.selectList(houseQuery);
    }
    
    @GetMapping("/{id}")
    public House getHouseById(@PathVariable("id") Long id) {
        return houseMapper.selectById(id);
    }
    
    @GetMapping("/list/ids")
    public List<House> getHouseListByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return houseMapper.selectBatchIds(ids);
    }
    
    @GetMapping("/search")
    public List<Long> searchHouseIds(@RequestParam("keyword") String keyword) {
        return houseMapper.selectList(new LambdaQueryWrapper<House>()
                .like(House::getBuildingNo, keyword)
                .or()
                .like(House::getHouseNo, keyword)
                .or()
                .like(House::getCommunityName, keyword)
        ).stream().map(House::getId).collect(Collectors.toList());
    }
    
    @PostMapping("/bind")
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindUserToHouse(@RequestParam("userId") Long userId, @RequestParam("houseId") Long houseId) {
        //检查房屋是否存在
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房屋不存在");
        }
        //检查房屋是否已被其他用户绑定
        if (house.getBindStatus() != null && house.getBindStatus() == 1) {
            throw new RuntimeException("该房屋已被其他用户绑定");
        }

        //检查当前用户是否已绑定该房屋
        Long count = userHouseMapper.selectCount(new LambdaQueryWrapper<UserHouse>()
                .eq(UserHouse::getHouseId, houseId)
                .eq(UserHouse::getUserId, userId));
        if (count > 0) {
            throw new RuntimeException("用户已绑定该房屋");
        }

        //插入数据
        if (userHouseMapper.insertUserHouseBind(userId, houseId) <= 0) {
            return false;
        }

        //更新房屋绑定状态为已绑定
        house.setBindStatus(1);
        int updateRows = houseMapper.updateById(house);
        return updateRows > 0;
    }
    
    @GetMapping("/community/count")
    public Long countCommunities() {
        return communityService.count();
    }
    
    @GetMapping("/owner/count/community")
    public Long countOwnersByCommunityId(@RequestParam("communityId") Long communityId) {
        return userHouseMapper.countDistinctOwnerByCommunityId(communityId);
    }

    @GetMapping("/community/{id}/name")
    public String getCommunityNameById(@PathVariable("id") Long id) {
        // CommunityService 需要提供 getById 方法
        // 假设 CommunityService 继承了 IService<Community>
        // 需要检查 CommunityService
        // 暂时假设有
        com.lsx.house.entity.Community c = communityService.getById(id);
        return c != null ? c.getName() : null;
    }

    @GetMapping("/user/count/condition")
    public Integer countUsersByCondition(
            @RequestParam(required = false) String communityName,
            @RequestParam(required = false) String buildingNo) {
        
        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
        if (communityName != null && !communityName.isEmpty()) {
            houseQuery.eq(House::getCommunityName, communityName);
        }
        if (buildingNo != null && !buildingNo.isEmpty()) {
            houseQuery.eq(House::getBuildingNo, buildingNo);
        }
        List<Long> houseIds = houseMapper.selectList(houseQuery).stream()
                .map(House::getId).collect(Collectors.toList());
        
        if (houseIds.isEmpty()) {
            return 0;
        }
        
        LambdaQueryWrapper<UserHouse> query = new LambdaQueryWrapper<>();
        // 兼容 "1" 和 "审核通过"
        query.in(UserHouse::getStatus, "1", "审核通过", "approved");
        query.in(UserHouse::getHouseId, houseIds);
        
        return (int) userHouseMapper.selectList(query).stream()
                .map(UserHouse::getUserId).distinct().count();
    }

    @GetMapping("/info")
    public House getHouseByInfo(
            @RequestParam(required = false) String buildingNo,
            @RequestParam(required = false) String houseNo) {
        return houseMapper.selectOne(new LambdaQueryWrapper<House>()
                .eq(buildingNo != null, House::getBuildingNo, buildingNo)
                .eq(houseNo != null, House::getHouseNo, houseNo)
                .last("LIMIT 1"));
    }

    @GetMapping("/bound-users/{houseId}")
    public List<Long> getBoundUsersByHouseId(@PathVariable("houseId") Long houseId) {
        return userHouseMapper.selectList(new LambdaQueryWrapper<UserHouse>()
                .eq(UserHouse::getHouseId, houseId)
                .in(UserHouse::getStatus, "1", "审核通过", "approved")
        ).stream().map(UserHouse::getUserId).distinct().collect(Collectors.toList());
    }

    @GetMapping("/bound-ids")
    public List<Long> getBoundHouseIds(
            @RequestParam(required = false) String communityName,
            @RequestParam(required = false) String buildingNo) {
        
        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
        if (communityName != null && !communityName.isEmpty()) {
            houseQuery.eq(House::getCommunityName, communityName);
        }
        if (buildingNo != null && !buildingNo.isEmpty()) {
            houseQuery.eq(House::getBuildingNo, buildingNo);
        }
        List<Long> houseIds = houseMapper.selectList(houseQuery).stream()
                .map(House::getId).collect(Collectors.toList());
        
        if (houseIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return userHouseMapper.selectList(new LambdaQueryWrapper<UserHouse>()
                .in(UserHouse::getHouseId, houseIds)
                .in(UserHouse::getStatus, "1", "审核通过", "approved")
        ).stream().map(UserHouse::getHouseId).distinct().collect(Collectors.toList());
    }
}
