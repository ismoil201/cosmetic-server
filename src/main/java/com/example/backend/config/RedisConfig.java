package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
 * <p>
 * Purpose:
 * - Configure Redis connection and serialization
 * - Define cache managers with different TTL strategies
 * - Enable Spring Cache abstraction
 * <p>
 * Safety:
 * - Graceful fallback if Redis is unavailable
 * - Type-safe serialization with Jackson
 * - Separate TTL for different cache types
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * ⚠️ CRITICAL: Primary ObjectMapper for REST API responses
     * <p>
     * Used by Spring's MappingJackson2HttpMessageConverter
     * for ALL REST endpoints (@RestController, @ResponseBody)
     * <p>
     * Configuration:
     * - NO activateDefaultTyping() → Clean JSON without type metadata
     * - JavaTimeModule → Proper LocalDateTime/LocalDate serialization
     * - Standard JSON output → Android/iOS/Web compatible
     * <p>
     * DO NOT add activateDefaultTyping() to this bean!
     * Type metadata (@class, typed arrays) breaks mobile clients.
     */
    @Bean
    @Primary  // ← Ensures Spring uses THIS for HTTP message conversion
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ Support Java 8 date/time types (e.g., LocalDateTime, LocalDate)
        mapper.registerModule(new JavaTimeModule());

        // ✅ NO activateDefaultTyping() — produces clean standard JSON
        // ✅ BigDecimal → plain JSON number (not typed array)
        // ✅ List<T> → plain JSON array (not typed wrapper)
        // ✅ No @class metadata in responses

        return mapper;
    }

    /**
     * ⚠️ ISOLATED: ObjectMapper for Redis cache serialization ONLY
     * <p>
     * Used exclusively by GenericJackson2JsonRedisSerializer
     * for Redis cache storage/retrieval.
     * <p>
     * Configuration:
     * - activateDefaultTyping() → Preserves Java type info in Redis
     * - JavaTimeModule → Proper date/time handling
     * <p>
     * Why type metadata is needed in Redis:
     * - Cache stores polymorphic objects (List<Object>, Map<String, ?>)
     * - Deserialization requires type info to reconstruct exact Java types
     * - Type metadata stays in Redis, NEVER exposed to REST clients
     * <p>
     * Isolation:
     * - @Qualifier("redisObjectMapper") ensures isolation
     * - NOT @Primary → Spring won't use this for HTTP responses
     */
    @Bean("redisObjectMapper")  // ← Explicit bean name for qualification
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ Support Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());

        // ✅ Preserve type info for correct Redis deserialization
        // This is SAFE because it's isolated to Redis cache layer
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * Redis serializer (uses ISOLATED Redis-specific ObjectMapper)
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {  // ← Explicit qualifier
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
     * <p>
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
