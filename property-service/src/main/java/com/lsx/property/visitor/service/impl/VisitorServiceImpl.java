package com.lsx.property.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.mapper.SysVisitorMapper;
import com.lsx.property.visitor.service.VisitorService;
import com.lsx.property.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.lsx.property.visitor.dto.VisitorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VisitorServiceImpl extends ServiceImpl<SysVisitorMapper, SysVisitor> implements VisitorService {
    
    @Autowired
    private UserServiceClient userServiceClient;

    private static final Logger log = LoggerFactory.getLogger(VisitorServiceImpl.class);

    @Override
    public Long apply(SysVisitor entity) {
        Long cid = UserContext.getCommunityId();
        entity.setCommunityId(cid);
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public IPage<SysVisitor> myList(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysVisitor> page = new Page<>(pageNum, pageSize);
        return this.page(page, new QueryWrapper<SysVisitor>().eq("user_id", userId).orderByDesc("create_time"));
    }

    @Override
    public IPage<VisitorDTO> adminList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<VisitorDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        
        log.info("瀵偓婵澧界悰?Mapper 閺屻儴顕? communityId={}, role={}, keyword={}, status={}", cid, role, keyword, status);
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) filterCid = cid;
            else filterCid = -1L;
        }
        
        try {
            IPage<VisitorDTO> result = baseMapper.selectAdminList(page, status, keyword, filterCid);
            // 閹靛濮╂繅顐㈠帠 ownerName (閻劍鍩涢惇鐔风杽婵挸鎮?
            result.getRecords().forEach(dto -> {
                if (dto.getUserId() != null) {
                    try {
                        String realName = userServiceClient.getRealNameById(dto.getUserId());
                        dto.setOwnerName(realName);
                    } catch (Exception e) {
                        log.warn("閼惧嘲褰囬悽銊﹀煕婵挸鎮曟径杈Е: userId={}", dto.getUserId());
                        dto.setOwnerName("閺堫亞鐓￠悽銊﹀煕");
                    }
                }
            });
            log.info("Mapper 閺屻儴顕楁潻鏂挎礀: size={}", result.getRecords().size());
            return result;
        } catch (Exception e) {
            log.error("Mapper 閹笛嗩攽婢惰精瑙?", e);
            throw e;
        }
    }

    @Override
    public boolean audit(Long id, String status, String remark) {
        SysVisitor v = this.getById(id);
        if (v == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || v.getCommunityId() == null || !cid.equals(v.getCommunityId())) {
                throw new RuntimeException("閺冪姵娼堢€光剝鐗抽崗未读电铂缁€鎯у隘鐠佸灝顓?);
            }
        }
        v.setStatus(status);
        v.setAuditRemark(remark);
        v.setUpdateTime(LocalDateTime.now());
        return this.updateById(v);
    }
}

