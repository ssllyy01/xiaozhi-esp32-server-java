package com.xiaozhi.communication.common;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 */
@Component
public class ConfigManager {

    // 当前jvm缓存所有的配置信息，后续要考虑通过标识符禁用map缓存，而仅使用redis缓存，避免脏数据问题
    private final ConcurrentHashMap<Integer, SysConfig> configCache = new ConcurrentHashMap<>();

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 获取配置
     *
     * @param configId 配置ID
     * @return SysConfig
     */
    public SysConfig getConfig(Integer configId) {
        // 从缓存中获取配置
        SysConfig config = configCache.get(configId);
        if (config == null) {
            // 如果缓存中没有，则从数据库中获取（接口方法带redis缓存）
            config = sysConfigService.selectConfigById(configId);
            if (config != null) {
                // 将配置放入缓存
                configCache.put(configId, config);
            }
        }
        return config;
    }
}
