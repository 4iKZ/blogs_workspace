package com.blog.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {

    @org.springframework.beans.factory.annotation.Value("${spring.redis.host}")
    private String host;

    @org.springframework.beans.factory.annotation.Value("${spring.redis.port}")
    private int port;

    @org.springframework.beans.factory.annotation.Value("${spring.redis.password}")
    private String password;

    /**
     * explicitly configure factory to guarantee host usage
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        org.springframework.data.redis.connection.RedisStandaloneConfiguration config = new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(org.springframework.data.redis.connection.RedisPassword.of(password));
        return new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(config);
    }

    /**
     * Config RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // Jackson serializer
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        
        // Visibility
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // JavaTimeModule
        mapper.registerModule(new JavaTimeModule());
        
        // Config options
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        serializer.setObjectMapper(mapper);
        
        // Set serializers
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * 专门用于 ZSet 操作的 StringRedisTemplate
     * 用于存储文章 ID（使用字符串序列化，避免 Jackson 序列化问题）
     */
    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        org.springframework.data.redis.core.StringRedisTemplate template = new org.springframework.data.redis.core.StringRedisTemplate(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}