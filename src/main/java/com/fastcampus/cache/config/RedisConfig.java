package com.fastcampus.cache.config;


import com.fastcampus.cache.domain.entity.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    RedisTemplate<String, User> uesrRedisTemplate(RedisConnectionFactory connectionFactory) {
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

        var template = new RedisTemplate<String, User>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, User.class));
        return template;
    }

    @Bean
    // Spring 빈으로 등록될 메서드를 정의합니다.
    RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        // 다형성 타입 유효성 검사를 위한 검증기를 설정합니다.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Object.class)
                .build();

        // ObjectMapper 인스턴스를 생성하고 다양한 설정을 적용합니다.
        var objectMapper = new ObjectMapper()
                // 알 수 없는 속성 때문에 역직렬화가 실패하지 않도록 설정합니다.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // Java 8 날짜 및 시간 모듈을 등록합니다.
                .registerModule(new JavaTimeModule())
                // 기본 타입 활성화를 통해 다형성 타입 처리를 활성화합니다.
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                // 날짜 키를 타임스탬프로 쓰지 않도록 설정합니다.
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

        // RedisTemplate 인스턴스를 생성합니다.
        var template = new RedisTemplate<String, Object>();
        // Redis 연결 공장을 설정합니다.
        template.setConnectionFactory(connectionFactory);
        // 키 시리얼라이저를 문자열 시리얼라이저로 설정합니다.
        template.setKeySerializer(new StringRedisSerializer());
        // 값 시리얼라이저를 GenericJackson2JsonRedisSerializer로 설정합니다.
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        // 설정된 RedisTemplate 인스턴스를 반환합니다.
        return template;
    }
}
