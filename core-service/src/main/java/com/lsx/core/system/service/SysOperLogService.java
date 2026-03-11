package com.lsx.core.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.system.entity.SysOperLog;

public interface SysOperLogService extends IService<SysOperLog> {
    /**
     * 清空操作日志
     */
    void cleanOperLog();
}
