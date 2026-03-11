package com.lsx.user.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.user.entity.User;
import com.lsx.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Resource
    private UserService userService;

    @GetMapping("/count")
    public Long countUsers() {
        return userService.count();
    }
    
    @GetMapping("/count/role")
    public Long countUsersByRole(@RequestParam("role") String role) {
        return userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, role));
    }

    @GetMapping("/{id}/realname")
    public String getRealNameById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        return user != null ? user.getRealName() : null;
    }
}
