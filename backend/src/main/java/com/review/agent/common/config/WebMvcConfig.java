package com.review.agent.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  
    /**  
     * 跨域配置  
     */  
    @Override  
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  
                .allowedHeaders("*")  
                .allowCredentials(true);  // 允许发送Cookie  
    }  
//    /**
//     * Cookie 配置
//     * @return CookieSerializer
//     */    @Bean
//    public CookieSerializer cookieSerializer(){
//        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
//        cookieSerializer.setSameSite(null);
//        return cookieSerializer;
//    }
//
//    /**
//     * 过滤器
//     * @return FilterRegistrationBean
//     */    @Bean
//    public FilterRegistrationBean<MyFilter> myFilterRegistration() {
//        FilterRegistrationBean<MyFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(new MyFilter());
//        registration.addUrlPatterns("/*");
//        registration.setName("myFilter");
//        registration.setOrder(1);
//        return registration;
//    }
}