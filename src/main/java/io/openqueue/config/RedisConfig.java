package io.openqueue.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * @author chenjing
 */
@Configuration
@AutoConfigureAfter(RedisConfig.class)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Serializable> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericFastJsonRedisSerializer());
        template.setHashValueSerializer(new GenericFastJsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {

        RedisSerializationContext<String, Serializable> serializationContext = RedisSerializationContext
                .<String, Serializable>newSerializationContext(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new GenericFastJsonRedisSerializer())
                .build();

        return new ReactiveRedisTemplate<>(redisConnectionFactory, serializationContext);
    }
}
