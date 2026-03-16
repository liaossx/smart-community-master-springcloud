package com.lsx.core.common.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Spring Security 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 核心安全配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("===== 加载 Security 配置：启用 JWT 登录校验 =====");

        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())

                // 使用无状态会话（JWT 必须这样）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置接口权限规则
                .authorizeHttpRequests(auth -> auth

                        /* ===== Swagger/Knife4j 放行 ===== */
                        .antMatchers(
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/**/v3/api-docs",
                                "/v2/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        /* ===== 登录注册放行 ===== */
                        .antMatchers(
                                "/api/user/login",
                                "/api/user/register",
                                "/api/common/**"
                        ).permitAll()

                        /* ===== 管理员（ADMIN）可访问 ===== ⭐ 先配置具体的admin路径 */
                        .antMatchers("/api/repair/admin/**", "/api/repair/stats/**").hasAnyRole("ADMIN", "super_admin")  // ⭐ 报修统计及管理
                        .antMatchers("/api/admin/user/**", "/api/admin/stats/**").hasAnyRole("ADMIN", "super_admin") // ⭐ 用户管理及系统统计
                        .antMatchers("/api/house/admin/**", "/api/house/community/admin/**").hasAnyRole("ADMIN", "super_admin")
                        .antMatchers("/api/system/config/**", "/api/monitor/operlog/**").hasAnyRole("ADMIN", "super_admin") // 系统配置及日志
                        .antMatchers("/api/parking/order/admin/**").hasAnyRole("ADMIN", "super_admin") // 停车订单管理
                        .antMatchers("/api/parking/space/admin/**").hasAnyRole("ADMIN", "super_admin") // 车位管理
                        .antMatchers("/api/parking/reserve/admin/**").hasAnyRole("ADMIN", "super_admin") // 车位预订管理
                        .antMatchers("/api/vehicle/admin/**", "/api/parking/vehicle/admin/**", "/api/vehicle/audit/**").hasAnyRole("ADMIN", "super_admin") // 车辆审核管理
                        .antMatchers("/api/house/updateUserHouseStatus").hasAnyRole("ADMIN", "super_admin")
                        .antMatchers("/api/fee/list", "/api/fee/generate", "/api/fee/admin/**").hasAnyRole("ADMIN", "super_admin") // 费用管理
                        
                        // 补充 访客、投诉、活动、公告 管理端统计及管理接口权限
                        .antMatchers("/api/visitor/list", "/api/visitor/admin/**", "/api/visitor/stats/**").hasAnyRole("ADMIN", "super_admin")
                        .antMatchers("/api/complaint/list", "/api/complaint/admin/**", "/api/complaint/stats/**").hasAnyRole("ADMIN", "super_admin")
                        .antMatchers("/api/notice/admin/**", "/api/notice/stats/**").hasAnyRole("ADMIN", "super_admin") // notice/list 移除管理员限制，供业主访问
                        .antMatchers("/api/activity/publish", "/api/activity/admin/**").hasAnyRole("ADMIN", "super_admin")
                        // 允许所有已认证用户（包括业主和管理员）查看活动列表
                        .antMatchers("/api/activity/list").authenticated() 
                        .antMatchers(org.springframework.http.HttpMethod.GET, "/api/activity/{id}").authenticated()
                        
                        /* ===== 访客、投诉 业主端接口 ===== */
                        .antMatchers("/api/visitor/apply", "/api/visitor/my").hasRole("OWNER")
                        .antMatchers("/api/complaint/submit", "/api/complaint/my").hasRole("OWNER")
                        .antMatchers("/api/activity/join").hasRole("OWNER")
                        .antMatchers("/api/vehicle/bind").hasRole("OWNER")
                        .antMatchers("/api/notice/list", "/api/notice/user/list", "/api/notice/unread-count", "/api/notice/*/read").hasRole("OWNER") // 业主可查看公告

                        /* ===== 普通业主（OWNER）可访问 ===== ⭐ 后配置通用路径 */
                        .antMatchers("/api/repair/**").hasRole("OWNER")  // ⭐ 放在后面！
                        .antMatchers("/api/user/bindUserToHouse").hasRole("OWNER")

                        /* ===== 其他接口需要登录 ===== */
                        .anyRequest().authenticated()
                )

                // 自定义无权限处理
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // 加载 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 禁用默认登录/注销
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
