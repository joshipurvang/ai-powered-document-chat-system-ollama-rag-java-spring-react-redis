package com.docuchat.config;

import com.docuchat.model.UserSession;
import com.docuchat.vectorstore.CustomRedisVectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for vector store and caching.
 *
 * 
 * @version 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * Creates ObjectMapper with Java 8 date/time support and type information.
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JSR310 module for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable default typing to preserve class information
        // This allows proper deserialization of UserSession and other classes
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

       // mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        return mapper;
    }

    /**
     * Creates Redis template for general caching.
     *
     * @param connectionFactory Redis connection factory
     * @param redisObjectMapper ObjectMapper with Java 8 date/time support
     * @return configured Redis template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use custom ObjectMapper with JSR310 and type information support
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates vector store using custom Redis implementation.
     *
     * @param redisTemplate Redis template
     * @param embeddingModel embedding model for vector generation
     * @return configured vector store
     */
    @Bean
    public VectorStore vectorStore(
            RedisTemplate<String, CustomRedisVectorStore.VectorEntry> redisTemplate,
            RedisTemplate<String, String> redisIndexTemplate,
            EmbeddingModel embeddingModel) {
        return new CustomRedisVectorStore(redisTemplate, redisIndexTemplate, embeddingModel);
    }





    /**
     * Creates text splitter for chunking documents.
     *
     * @param appProperties application properties
     * @return configured text splitter
     */
    @Bean
    public TokenTextSplitter textSplitter(AppProperties appProperties) {
        return new TokenTextSplitter(
                appProperties.getRag().getChunkSize(),
                appProperties.getRag().getChunkOverlap(),
                5,    // Min chunk size in tokens
                10000, // Max chunk size in tokens
                true   // Keep separator
        );
    }
    @Bean
    public RedisTemplate<String, UserSession> userSessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, UserSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<UserSession> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, UserSession.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public RedisTemplate<String, CustomRedisVectorStore.VectorEntry> vectorEntryRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, CustomRedisVectorStore.VectorEntry> template =
                new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<CustomRedisVectorStore.VectorEntry> serializer =
                new Jackson2JsonRedisSerializer<>(CustomRedisVectorStore.VectorEntry.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }



    /*@Bean
    public RedisTemplate<String, String> stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }*/


}