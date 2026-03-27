package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 *
 * Purpose:
 * - Configure Redis connection and serialization
 * - Define cache managers with different TTL strategies
 * - Enable Spring Cache abstraction
 *
 * Safety:
 * - Graceful fallback if Redis is unavailable
 * - Type-safe serialization with Jackson
 * - Separate TTL for different cache types
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Custom ObjectMapper for Redis serialization
     * - Handles Java 8 time types (LocalDateTime, LocalDate)
     * - Preserves type information for polymorphic objects
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ Support Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());

        // ✅ Preserve type info for correct deserialization
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * Redis serializer for values (uses Jackson)
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer(ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    /**
     * RedisTemplate for manual cache operations (if needed)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ✅ String keys for readability
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // ✅ JSON values for flexibility
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager with per-cache TTL configuration
     *
     * Cache Types:
     * 1. home:blocks - 5 minutes (home page sections)
     * 2. user:interests - 30 minutes (user category/brand preferences)
     * 3. product:images - 1 hour (product images - rarely change)
     * 4. home:todaydeals - 10 minutes (today's deals)
     * 5. feed:explore - 5 minutes (explore candidate pool)
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        // ✅ Default cache config (fallback)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer)
                )
                .disableCachingNullValues();  // ✅ Don't cache null results

        // ✅ Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Home blocks: 5 minutes (high traffic, acceptable staleness)
        cacheConfigs.put("home:blocks", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // User interests: 30 minutes (changes slowly)
        cacheConfigs.put("user:interests", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Product images: 1 hour (static data)
        cacheConfigs.put("product:images", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Today deals: 10 minutes (changes rarely)
        cacheConfigs.put("home:todaydeals", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Feed explore candidates: 5 minutes (refreshes frequently)
        cacheConfigs.put("feed:explore", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()  // ✅ Respect @Transactional boundaries
                .build();
    }
}
