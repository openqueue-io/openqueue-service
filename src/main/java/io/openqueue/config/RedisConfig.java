package io.openqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * @author chenjing
 */
@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        RedisSerializationContext<String, Serializable> serializationContext = RedisSerializationContext
                .<String, Serializable>newSerializationContext(stringRedisSerializer)
                .hashKey(stringRedisSerializer)
                .hashValue(new GenericJackson2JsonRedisSerializer())
                .build();

        return new ReactiveRedisTemplate<>(redisConnectionFactory, serializationContext);
    }
}
