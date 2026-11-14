package com.github.cauanHalison.scheduling_api.config.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String EVENT_TYPES_CACHE = "event-types";
    public static final String AVAILABLE_SLOTS_CACHE = "available-slots";
    public static final String AVAILABLE_DAYS_CACHE = "available-days";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType eventTypeListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, EventTypeResponse.class);

        Jackson2JsonRedisSerializer<List<EventTypeResponse>> eventTypesSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, eventTypeListType);

        Jackson2JsonRedisSerializer<AvailableDaysResponse> availableDaysSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, AvailableDaysResponse.class);

        Jackson2JsonRedisSerializer<AvailableSlotsResponse> availableSlotsSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, AvailableSlotsResponse.class);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        RedisCacheConfiguration eventTypesCacheConfig = defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(eventTypesSerializer));

        RedisCacheConfiguration availableDaysCacheConfig = defaultCacheConfig
                .entryTtl(Duration.ofMinutes(15))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(availableDaysSerializer));

        RedisCacheConfiguration availableSlotsCacheConfig = defaultCacheConfig
                .entryTtl(Duration.ofMinutes(15))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(availableSlotsSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration(EVENT_TYPES_CACHE, eventTypesCacheConfig)
                .withCacheConfiguration(AVAILABLE_DAYS_CACHE, availableDaysCacheConfig)
                .withCacheConfiguration(AVAILABLE_SLOTS_CACHE, availableSlotsCacheConfig)
                .build();
    }
}
