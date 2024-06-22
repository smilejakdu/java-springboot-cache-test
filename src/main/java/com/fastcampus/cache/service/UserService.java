package com.fastcampus.cache.service;

import com.fastcampus.cache.domain.entity.RedisHashUser;
import com.fastcampus.cache.domain.entity.User;
import com.fastcampus.cache.domain.repository.RedisHashUserRepository;
import com.fastcampus.cache.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.fastcampus.cache.config.CacheConfig.CACHE1;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisHashUserRepository redisHashUserRepository;

    private final RedisTemplate<String, Object> objectRedisTemplate;

    public User getUser(final Long id) {
        var key = "users:%d".formatted(id);
        var cachedUser = objectRedisTemplate.opsForValue().get(key);
        if (cachedUser != null) { // 만약 캐시에 존재한다면
            return (User) cachedUser;
        }

        User user = userRepository.findById(id).orElseThrow();
        objectRedisTemplate.opsForValue().set(key, user, Duration.ofSeconds(30)); // Cache for 30 seconds
        return user;
    }

    public RedisHashUser getUser2(final Long id) {
        return redisHashUserRepository.findById(id).orElseGet(() -> createAndCacheRedisHashUser(id));
    }

    private RedisHashUser createAndCacheRedisHashUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        RedisHashUser redisHashUser = RedisHashUser.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return redisHashUserRepository.save(redisHashUser);
    }

    @Cacheable(cacheNames = CACHE1, key = "'user:' + #id")
    public User getUser3(final Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
