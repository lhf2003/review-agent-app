package com.review.agent.config;

import com.review.agent.common.security.JwtAuthenticationEntryPoint;
import com.review.agent.common.security.JwtAuthenticationFilter;
import com.review.agent.common.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 配置 JWT 认证、密码加密、CORS、URL 访问控制
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RateLimitFilter rateLimitFilter;

    /**
     * 密码加密器
     * 使用 BCrypt 算法，强度默认为 10
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security 过滤器链配置
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（JWT 无需 CSRF 保护）
                .csrf(AbstractHttpConfigurer::disable)

                // 配置会话管理（无状态，JWT 不需要会话）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                )

                // 配置 CORS
                .cors(org.springframework.security.config.Customizer.withDefaults())

                // 配置异常处理（未认证请求）
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 配置 URL 访问控制
                .authorizeHttpRequests(authorize -> authorize
                        // 允许匿名访问的路径（支持直接访问和前端代理两种方式）
                        .requestMatchers(
                                // 直接访问后端的路径
                                "/user/login",
                                "/user/register",
                                "/user/forgot-password",
                                "/user/test-token",
                                // 前端代理的路径
                                "/api/user/login",
                                "/api/user/register",
                                "/api/user/forgot-password",
                                "/api/user/test-token",
                                // 错误页面
                                "/error"
                        ).permitAll()

                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )

                // 添加限流过滤器（在最前面）
                .addFilterBefore(
                        rateLimitFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                )

                // 添加 JWT 认证过滤器
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS 配置
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 允许所有源
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
