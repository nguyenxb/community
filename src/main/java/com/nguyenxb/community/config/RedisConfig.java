package com.nguyenxb.community.config;

import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    // 要注入 连接redis 的数据库的工厂才能访问 redis数据库
    @Bean
    public RedisTemplate<String,Object>  redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String,Object> template =  new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 设置string 的 key的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        // 设置String的 value的序列化方式 , 一般设为json
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

}
