package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.parking.dto.ParkingCarAuditDTO;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingPlateMapper;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.vo.ParkingCarAuditVO;
import com.lsx.parking.client.UserServiceClient;
import com.lsx.parking.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.VehicleBindDTO;
import com.lsx.parking.entity.Vehicle;
import com.lsx.parking.mapper.VehicleMapper;
import com.lsx.parking.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Service
public class VehicleServiceImpl
        extends ServiceImpl<VehicleMapper, Vehicle>
        implements VehicleService {

    @Autowired
    private ParkingPlateMapper plateMapper;
    @Autowired
    private ParkingSpaceMapper spaceMapper;
    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    @Transactional
    public void bindVehicle(VehicleBindDTO dto) {

        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.hasText(dto.getPlateNo(), "车牌鍙蜂笉鑳戒负绌?);
        Assert.notNull(dto.getSpaceId(), "璇烽€夋嫨鍏宠仈车位");

        // 1. 淇濆瓨车辆鍩烘湰淇℃伅锛堝鏋滀笉瀛樺湪锛?        Vehicle exist = this.lambdaQuery()
                .eq(Vehicle::getPlateNo, dto.getPlateNo())
                .one();

        if (exist != null) {
            if (!exist.getUserId().equals(dto.getUserId())) {
                throw new RuntimeException("璇ヨ溅鐗屽凡琚叾浠栫敤鎴风粦瀹?);
            }
            // 宸插瓨鍦ㄤ笖鏄嚜宸辩殑锛屾洿鏂颁俊鎭?            if (dto.getColor() != null) exist.setColor(dto.getColor());
            if (dto.getBrand() != null) exist.setBrand(dto.getBrand());
            this.updateById(exist);
        } else {
            // 鏂板车辆
            Vehicle vehicle = new Vehicle();
            vehicle.setUserId(dto.getUserId());
            vehicle.setPlateNo(dto.getPlateNo());
            vehicle.setBrand(dto.getBrand());
            vehicle.setColor(dto.getColor());
            vehicle.setStatus("ACTIVE");
            vehicle.setCreateTime(LocalDateTime.now());
            vehicle.setUpdateTime(LocalDateTime.now());
            this.save(vehicle);
        }

        // 2. 绑定鍒版寚瀹氳溅浣?(biz_parking_space_plate)
        // 妫€查询车位涓嬫槸鍚﹀凡绑定璇ヨ溅鐗?        Long count = plateMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParkingSpacePlate>()
                .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                .ne(ParkingSpacePlate::getStatus, "REJECTED")); // 拒绝鐨勫彲浠ラ噸鎻?
        if (count > 0) {
            throw new RuntimeException("璇ヨ溅浣嶅凡绑定鎴栨鍦ㄥ鏍告车牌");
        }

        // 鎻掑叆车位-车牌鍏宠仈记录锛岀姸鎬佽涓?PENDING
        ParkingSpacePlate plateBind = new ParkingSpacePlate();
        plateBind.setSpaceId(dto.getSpaceId());
        plateBind.setUserId(dto.getUserId());
        plateBind.setPlateNo(dto.getPlateNo());
        plateBind.setStatus("PENDING"); 
        plateBind.setCreateTime(LocalDateTime.now());
        plateBind.setUpdateTime(LocalDateTime.now());
        
        plateMapper.insert(plateBind);
    }

    @Override
    public IPage<ParkingCarAuditVO> listAudit(String status, Integer pageNum, Integer pageSize) {
        Page<ParkingSpacePlate> page = new Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParkingSpacePlate> query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            query.eq(ParkingSpacePlate::getStatus, status);
        }
        query.orderByDesc(ParkingSpacePlate::getCreateTime);
        
        IPage<ParkingSpacePlate> platePage = plateMapper.selectPage(page, query);
        
        return platePage.convert(item -> {
            ParkingCarAuditVO vo = new ParkingCarAuditVO();
            vo.setId(item.getId());
            vo.setPlateNo(item.getPlateNo());
            vo.setStatus(item.getStatus());
            vo.setRejectReason(item.getRejectReason());
            vo.setCreateTime(item.getCreateTime());
            
            try {
                // 填充申请人
                if (item.getUserId() != null) {
                    UserDTO user = userServiceClient.getUserById(item.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getRealName());
                    }
                }
                
                // 濉厖车位鍙?                if (item.getSpaceId() != null) {
                    ParkingSpace space = spaceMapper.selectById(item.getSpaceId());
                    if (space != null) {
                        vo.setSpaceNo(space.getSpaceNo());
                    }
                }
            } catch (Exception e) {
                // 鍗充娇鍏宠仈查询失败锛屼篃涓嶅奖鍝嶄富鏁版嵁鏄剧ず
                e.printStackTrace();
            }
            
            return vo;
        });
    }

    @Override
    @Transactional
    public void auditCar(ParkingCarAuditDTO dto) {
        ParkingSpacePlate plate = plateMapper.selectById(dto.getId());
        if (plate == null) {
            throw new RuntimeException("申请记录不存在?);
        }
        if (!"PENDING".equals(plate.getStatus())) {
            throw new RuntimeException("璇ヨ褰曞凡瀹℃牳");
        }
        
        plate.setStatus(dto.getStatus());
        if ("REJECTED".equals(dto.getStatus())) {
            plate.setRejectReason(dto.getRejectReason());
        }
        plate.setUpdateTime(LocalDateTime.now());
        
        plateMapper.updateById(plate);
    }
}
