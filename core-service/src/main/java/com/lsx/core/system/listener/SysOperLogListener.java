package com.lsx.core.system.listener;

import cn.hutool.core.bean.BeanUtil;
import com.lsx.core.common.dto.SysOperLogDTO;
import com.lsx.core.common.event.OperationLogEvent;
import com.lsx.core.system.entity.SysOperLog;
import com.lsx.core.system.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 异步监听日志事件
 */
@Component
@Slf4j
public class SysOperLogListener {

    @Resource
    private SysOperLogService sysOperLogService;

    @Async // 异步执行，不影响业务响应速度
    @EventListener
    public void saveSysLog(OperationLogEvent event) {
        try {
            SysOperLogDTO logDto = event.getOperLog();
            SysOperLog sysOperLog = new SysOperLog();
            BeanUtil.copyProperties(logDto, sysOperLog);
            sysOperLogService.save(sysOperLog);
        } catch (Exception e) {
            log.error("保存操作日志失败:{}", e.getMessage());
            e.printStackTrace();
        }
    }
}
