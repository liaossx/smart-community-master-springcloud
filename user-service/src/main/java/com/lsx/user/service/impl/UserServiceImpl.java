package com.lsx.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.JwtUtil;
import com.lsx.core.common.Util.UserContext;
import com.lsx.user.client.ParkingServiceClient;
import com.lsx.user.client.HouseServiceClient;
import com.lsx.user.dto.external.HouseDTO;
import com.lsx.user.dto.external.VehicleDTO;
import com.lsx.user.dto.*;
import com.lsx.user.vo.LoginResult;
import com.lsx.user.vo.RegisterResult;
import com.lsx.user.entity.User;
import com.lsx.user.mapper.UserMapper;
import com.lsx.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private ParkingServiceClient parkingServiceClient;
    
    @Resource
    private HouseServiceClient houseServiceClient;

    @Override
    public LoginResult login(LoginDto loginDto) {
        String username = loginDto.getUsername();
        String plainPassword = loginDto.getPassword();
        // 1. 按用户名查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = baseMapper.selectOne(queryWrapper);

        // 2. 校验用户是否存在和密码是否正确
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 密码校验
        String encryptedPassword = user.getPassword();
        boolean isPasswordValid = BCrypt.checkpw(plainPassword, encryptedPassword);
        if (!isPasswordValid) {
            throw new RuntimeException("密码错误");
        }

        // 3. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getCommunityId());

        // 4. 返回登录结果（包含Token）
        return new LoginResult(user, token);
    }
    @Override
    @CacheEvict(cacheNames = "userList", allEntries = true)
    public RegisterResult  register(RegisterDto registerDto) {
        // 自动转换 superadmin -> super_admin 以匹配系统权限标识
        if ("superadmin".equalsIgnoreCase(registerDto.getRole())) {
            registerDto.setRole("super_admin");
        }

        //校验角色必须是"owner"、"admin"、"super_admin" 或 "worker"
        String role = registerDto.getRole();
        if(role == null || (!"owner".equals(role) && !"admin".equals(role) && !"super_admin".equals(role) && !"worker".equals(role))){
            throw new RuntimeException("角色必须是'owner'、'admin'、'super_admin' 或 'worker'");
        }
        // 1. 检查用户名是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDto.getUsername());
        User existingUser = baseMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        //对密码进行加密
        registerDto.setPassword(jwtUtil.encryptPassword(registerDto.getPassword()));
        User user = new User();
        BeanUtil.copyProperties(registerDto, user);
        // 设置社区ID
        user.setCommunityId(registerDto.getCommunityId());
        
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 2. 插入用户数据
        if (baseMapper.insert(user) <= 0) {
            throw new RuntimeException("注册失败，请重试");
        }
        // 3. 返回注册结果
        RegisterResult result = new RegisterResult();
        BeanUtils.copyProperties(user, result);

        return result;

    }
    //绑定用户于房屋
    @Transactional(rollbackFor = Exception.class)
    @Override
    @CacheEvict(cacheNames = "userDetail", key = "#userId")
    public void bindUserToHouse(Long userId,Long houseId){
        //非空校验
        if (userId == null){
            throw new RuntimeException("用户ID不能为空");
        }
        if (houseId == null){
            throw new RuntimeException("房屋ID不能为空");
        }
        
        // 调用 house-service 进行绑定
        try {
            Boolean result = houseServiceClient.bindUserToHouse(userId, houseId);
            if (!result) {
                throw new RuntimeException("绑定失败，请重试");
            }
        } catch (Exception e) {
            throw new RuntimeException("绑定失败：" + e.getMessage());
        }
    }

    @Override
    @Cacheable(cacheNames = "userList", key = "#pageNum + '-' + #pageSize + '-' + #keyword + '-' + #role")
    public Page<UserDTO> getUserList(Integer pageNum, Integer pageSize, String keyword, String role) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // --- 权限过滤逻辑 ---
        String currentRole = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(currentRole)) {
            // 超级管理员：不做额外限制
        } else {
            // 普通管理员/业主：必须有社区ID
            if (currentCommunityId == null) {
                // 如果没有绑定社区，则什么都查不到（或者可以抛出异常）
                // 这里为了安全，添加一个不可能成立的条件
                wrapper.eq(User::getId, -1L);
            } else {
                wrapper.eq(User::getCommunityId, currentCommunityId);
            }
        }
        // -----------------
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> userPage = baseMapper.selectPage(page, wrapper);
        
        // Convert to DTO
        Page<UserDTO> dtoPage = new Page<>(pageNum, pageSize);
        dtoPage.setTotal(userPage.getTotal());
        dtoPage.setPages(userPage.getPages());
        dtoPage.setCurrent(userPage.getCurrent());
        dtoPage.setSize(userPage.getSize());
        
        List<UserDTO> dtoList = userPage.getRecords().stream().map(user -> {
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto);
            return dto;
        }).collect(Collectors.toList());
        
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    @Cacheable(cacheNames = "userDetail", key = "#userId")
    public UserDetailDTO getUserDetail(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // --- 权限检查 ---
        String currentRole = UserContext.getRole();
        Long currentUserId = UserContext.getCurrentUserId();
        Long currentCommunityId = UserContext.getCommunityId();
        
        // 如果是超级管理员，或者是用户查看自己，则放行
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        boolean isSuperAdmin = "super_admin".equalsIgnoreCase(currentRole);
        
        if (!isSuperAdmin && !isSelf && currentCommunityId != null) {
            if (user.getCommunityId() == null || !currentCommunityId.equals(user.getCommunityId())) {
                throw new RuntimeException("无权查看其他社区用户详情");
            }
        }
        // -----------------
        
        UserDetailDTO dto = new UserDetailDTO();
        BeanUtils.copyProperties(user, dto);
        
        // 1. 获取绑定的房屋
        // 调用 Feign Client 获取
        List<HouseDTO> houses = houseServiceClient.getHousesByUserId(userId);
        
        List<UserDetailDTO.BoundHouse> boundHouses = new ArrayList<>();
        if (houses != null && !houses.isEmpty()) {
            boundHouses = houses.stream().map(house -> {
                UserDetailDTO.BoundHouse bh = new UserDetailDTO.BoundHouse();
                bh.setHouseId(house.getId());
                bh.setCommunityName(house.getCommunityName());
                bh.setBuildingNo(house.getBuildingNo());
                bh.setHouseNo(house.getHouseNo());
                return bh;
            }).collect(Collectors.toList());
        }
        dto.setHouses(boundHouses);
        
        // 2. 获取车辆信息
        // 调用 Feign Client 获取
        List<VehicleDTO> vehicles = parkingServiceClient.getVehiclesByUserId(userId);
        
        List<UserDetailDTO.BoundVehicle> boundVehicles = new ArrayList<>();
        if (vehicles != null && !vehicles.isEmpty()) {
            boundVehicles = vehicles.stream().map(vehicle -> {
                UserDetailDTO.BoundVehicle bv = new UserDetailDTO.BoundVehicle();
                bv.setVehicleId(vehicle.getId());
                bv.setPlateNo(vehicle.getPlateNo());
                bv.setStatus(vehicle.getStatus());
                return bv;
            }).collect(Collectors.toList());
        }
        dto.setVehicles(boundVehicles);
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"userDetail", "userList"}, key = "#dto.userId", allEntries = true)
    public void adminUpdateUser(AdminUpdateUserDTO dto) {
        User user = baseMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // --- 权限检查 ---
        String currentRole = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(currentRole) && currentCommunityId != null) {
            if (user.getCommunityId() == null || !currentCommunityId.equals(user.getCommunityId())) {
                throw new RuntimeException("无权修改其他社区用户");
            }
        }
        // -----------------
        
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getRole())) {
            String role = dto.getRole();
            user.setRole("user".equalsIgnoreCase(role) ? "owner" : role);
        }
        
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }

    @Override
    @CacheEvict(cacheNames = {"userDetail", "userList"}, key = "#userId", allEntries = true)
    public void updateStatus(Long userId, Integer status) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // --- 权限检查 ---
        String currentRole = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(currentRole) && currentCommunityId != null) {
            if (user.getCommunityId() == null || !currentCommunityId.equals(user.getCommunityId())) {
                throw new RuntimeException("无权操作其他社区用户");
            }
        }
        // -----------------
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }

    @Override
    @CacheEvict(cacheNames = {"userDetail", "userList"}, key = "#userId", allEntries = true)
    public void resetPassword(Long userId, String newPassword) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // --- 权限检查 ---
        String currentRole = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(currentRole) && currentCommunityId != null) {
            if (user.getCommunityId() == null || !currentCommunityId.equals(user.getCommunityId())) {
                throw new RuntimeException("无权操作其他社区用户");
            }
        }
        // -----------------
        
        String pwd = StringUtils.hasText(newPassword) ? newPassword : "123456";
        user.setPassword(jwtUtil.encryptPassword(pwd));
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }

}


