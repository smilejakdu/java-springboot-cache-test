# java-springboot-cache-test

## 1. Introduction
This project is a simple example of how to use cache in a Spring Boot application.

## 2. Technologies

- Java 17
- Spring Boot 3.3.1
- Mysql 8.3.0
- Redis 7.2.5
    - redis monitoring
    - docker exec -it b93b9d984faf redis-cli monitor
    - docker stats b93b9d984faf

## 3. Setup
fist you have to settings build.gradle file to use the dependencies below:

```java
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.mysql:mysql-connector-j'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

and then create application.yml file

```yml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  datasource:
    url: jdbc:mysql://localhost:3307/cache?useSSL=false&serverTimezone=Asia/Seoul
    username: root
    password: 1234
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100
```


## 4. Run
Run the project and access the URL below:
http://localhost:8080

## 5. Explanation

RedisTemplate을 사용하여 Redis에 데이터를 저장하고 조회하는 방법을 알아보겠습니다.
```java
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
    RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Object.class)
                .build();
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }
}
```
settings RedisConfig to use RedisTemplate to save and retrieve data in Redis.
after that, we will test a simple query.
UserService.java
```java
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
}
```

## 6. Spring Cache
if you search for Spring Cache settings, you will find it.\n
When you search for cache, you will find Spring cache abstraction.\n
To use Spring Cache, you need to add `spring-boot-starter-cache` and `@EnableCaching`.\n

```java
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }
}
```

```shell
implementation 'org.springframework.boot:spring-boot-starter-cache'
```

The usage is simple. You just need to use `@Cacheable`, `@CacheEvict`, and `@CachePut`.

- `@Cacheable` returns cached data if it exists; if it doesn't, it executes the method and stores the result in the cache.
- `@CacheEvict` is an annotation used to clear the cache.
- `@CachePut` is an annotation used to store data in the cache.

```java
    @Cacheable(cacheNames = CACHE1, key = "'user:' + #id")
    public User getUser3(final Long id) {
        return userRepository.findById(id).orElseThrow();
    }
```

This is how to use caching with `@Cacheable`.\n
However, this method has the inconvenience of having to specify the cache key directly each time the cache is used.\n
To use dynamic data caching, you can use a `keyGenerator`.

## performanceTest
brew install vegeta
touch request1.txt
```shell
GET http://localhost:8080/user/1
GET http://localhost:8080/user/2
GET http://localhost:8080/user/3
```
```shell
vegeta attack -duration=30s -rate=5000/1s -targets=request1.txt -workers=100 | tee v_results.bin | vegeta report
```