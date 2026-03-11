package com.lsx.core.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.system.entity.SysOperLog;
import com.lsx.core.system.mapper.SysOperLogMapper;
import com.lsx.core.system.service.SysOperLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Resource
    private SysOperLogMapper operLogMapper;

    @Override
    public void cleanOperLog() {
        operLogMapper.delete(new QueryWrapper<>());
    }
}
