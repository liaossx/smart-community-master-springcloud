package com.lsx.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Result.Result;
import com.lsx.system.entity.SysConfig;
import com.lsx.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/system/config")
@Tag(name = "系统参数配置接口")
public class SysConfigController {

    @Resource
    private SysConfigService configService;

    @GetMapping("/list")
    @Operation(summary = "根据参数键名查询参数值")
    public Result<String> getConfigValue(@RequestParam("configKey") String configKey) {
        String value = configService.getValue(configKey);
        return Result.success(value);
    }
    
    @GetMapping("/all")
    @Operation(summary = "获取所有配置")
    public Result<List<SysConfig>> listAll() {
        return Result.success(configService.list());
    }

    @PostMapping
    @Operation(summary = "新增配置")
    public Result<Boolean> add(@RequestBody SysConfig config) {
        return Result.success(configService.save(config));
    }

    @PutMapping
    @Operation(summary = "修改配置")
    public Result<Boolean> update(@RequestBody SysConfig config) {
        return Result.success(configService.updateById(config));
    }
}
