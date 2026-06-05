package com.biyesheji.onlinehomework.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置类
 *
 * 描述：使用 Caffeine 作为本地缓存（配置文件名为 RedisConfig，但实际使用的是 Caffeine）
 * 用于缓存用户查询结果等数据，提高性能
 *
 * @Configuration: 标记为 Spring 配置类
 * @EnableCaching: 启用 Spring 缓存支持
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 配置缓存管理器
     * 使用 Caffeine 作为本地缓存实现
     *
     * @return 缓存管理器 Bean
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 缓存最大条目数
                .maximumSize(500)
                // 30 分钟未访问则过期
                .expireAfterAccess(30, TimeUnit.MINUTES)
                // 记录缓存统计信息
                .recordStats());

        return cacheManager;
    }
}
