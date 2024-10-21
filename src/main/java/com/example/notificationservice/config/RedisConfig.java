package com.example.notificationservice.config;

import com.example.notificationservice.entity.NotificationEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("notifications");
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationEntity> reactiveRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        var valueSerializer = new Jackson2JsonRedisSerializer<>(NotificationEntity.class);
        var serializationContext = RedisSerializationContext
                .<String, NotificationEntity>newSerializationContext(RedisSerializer.string())
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

    @Bean
    public ReactiveRedisMessageListenerContainer redisMessageListenerContainer(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        return new ReactiveRedisMessageListenerContainer(reactiveRedisConnectionFactory);
    }
}
