package com.xiaozhi.common.config;

import com.xiaozhi.common.interceptor.AuthenticationInterceptor;
import com.xiaozhi.common.interceptor.LogInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import jakarta.annotation.Resource;

import java.io.File;

@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {

    @Resource
    private LogInterceptor logInterceptor;

    @Resource
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/register",
                    "/api/device/ota",
                    "/audio/**",
                    "/uploads/**",
                    "/avatar/**",
                    "/ws/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 获取项目根目录的绝对路径
            String basePath = new File("").getAbsolutePath();

            // 音频文件存储在项目根目录下的audio文件夹中
            String audioPath = "file:" + basePath + File.separator + "audio" + File.separator;

            // 配置资源映射
            registry.addResourceHandler("/audio/**")
                    .addResourceLocations(audioPath);

            super.addResourceHandlers(registry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置路径匹配参数
     */
    @Override
    @SuppressWarnings("deprecation") // 暂时抑制过时警告
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 使用推荐的方法设置尾部斜杠匹配
        configurer.setUseTrailingSlashMatch(true);
    }
}