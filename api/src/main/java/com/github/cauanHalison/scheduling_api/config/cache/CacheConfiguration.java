package com.github.cauanHalison.scheduling_api.config.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String EVENT_TYPES_CACHE = "event-types";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType eventTypeListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, EventTypeResponse.class);

        Jackson2JsonRedisSerializer<List<EventTypeResponse>> eventTypesSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, eventTypeListType);

        org.springframework.data.redis.cache.RedisCacheConfiguration eventTypesCacheConfig =
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(30))
                        .disableCachingNullValues()
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(eventTypesSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration(EVENT_TYPES_CACHE, eventTypesCacheConfig)
                .build();
    }
}
