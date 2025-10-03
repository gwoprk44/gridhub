package com.gridhub.gridhub.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
public class RedisCacheConfig {

    /**
     * 캐싱을 포함하여 프로젝트 전역에서 사용할 기본 ObjectMapper를 설정.
     * @Primary 어노테이션을 통해 Spring Boot의 자동 설정보다 우선권을 갖는다.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule());
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf) {
        // Redis 캐시의 기본 설정을 정의 (TTL 30분)
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .entryTtl(Duration.ofMinutes(30L));

        // === 캐시 영역별 TTL 설정을 위한 Map 생성 ===
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // "leaderboard" 캐시는 TTL을 5분으로 별도 설정
        cacheConfigurations.put("leaderboard", redisCacheConfiguration.entryTtl(Duration.ofMinutes(5L)));

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(cf)
                .cacheDefaults(redisCacheConfiguration) // 기본 설정
                .withInitialCacheConfigurations(cacheConfigurations) // 캐시 영역별 커스텀 설정 적용
                .build();
    }
}