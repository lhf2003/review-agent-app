package com.review.agent.common.config;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedissonConfig {

    private String host;
    private String port;
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置  
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(redisAddress).setDatabase(database);

        //2.创建Redisson实例
        return Redisson.create(config);
    }

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    /**
     * Graph的检查点持久化配置
     * @param redisSaver redis保存器
     * @return 检查点持久化配置
     */
    @Bean
    public SaverConfig saverConfig(RedisSaver redisSaver) {
        return SaverConfig.builder()
                .register(redisSaver)
                .build();
    }


}