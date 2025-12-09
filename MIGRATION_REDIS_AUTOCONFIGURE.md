# Spring Boot Redis Autoconfiguration Migration

## Summary

Successfully migrated from manual Redis configuration to Spring Boot's autoconfiguration for
cleaner, more maintainable code.

## Changes Made

### 1. LogStore.java - Simplified Redis Template Usage ✅

**Before (Manual Configuration):**

```java

@Autowired
private RedisConnectionFactory redisConnectionFactory;

private StringRedisTemplate stringRedisTemplate;

@PostConstruct
public void init() {
  stringRedisTemplate = new StringRedisTemplate();
  stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
  stringRedisTemplate.afterPropertiesSet();
  logEntries = new DefaultRedisMap<>(LOGSTORE, stringRedisTemplate);
}
```

**After (Spring Boot Autoconfiguration):**

```java

@Autowired
private StringRedisTemplate stringRedisTemplate; // Auto-configured by Spring Boot

@PostConstruct
public void init() {
  // Use Spring Boot's auto-configured StringRedisTemplate
  logEntries = new DefaultRedisMap<>(LOGSTORE, stringRedisTemplate);
}
```

**Benefits:**

- ✅ **3 lines removed** from `@PostConstruct`
- ✅ No manual factory wiring needed
- ✅ No manual initialization (`afterPropertiesSet()`)
- ✅ Removed `RedisConnectionFactory` dependency
- ✅ Cleaner, more idiomatic Spring Boot code

### 2. Property Files - Already Using Spring Boot Conventions ✅

#### application.properties

```properties
# Spring Boot Redis Autoconfiguration
# Autoconfigures: RedisConnectionFactory, StringRedisTemplate, RedisTemplate
spring.data.redis.timeout=2000ms
spring.cache.redis.time-to-live=86400000
# Redis Sentinel Configuration (if needed in production)
# spring.data.redis.sentinel.master=master
# spring.data.redis.sentinel.nodes=redis-sentinel-1:26379,redis-sentinel-2:26379
```

#### application-dev.properties

```properties
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=2000ms
spring.cache.redis.time-to-live=86400000
```

#### application-test.properties

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**Already using Spring Boot standard properties!** ✅ No changes needed.

### 3. Dependencies - Already Configured ✅

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

This starter provides:

- ✅ `RedisConnectionFactory` (Lettuce by default)
- ✅ `RedisTemplate<Object, Object>`
- ✅ `StringRedisTemplate`
- ✅ Connection pooling (via Lettuce)
- ✅ Serialization configuration

## What Spring Boot Now Handles Automatically

Spring Boot's `RedisAutoConfiguration` provides:

1. **RedisConnectionFactory**
    - Auto-configured from `spring.data.redis.*` properties
    - Uses Lettuce client by default (better performance than Jedis)
    - Connection pooling enabled by default

2. **StringRedisTemplate**
    - Pre-configured for String key/value operations
    - Ready to use without manual setup
    - Proper serialization for strings

3. **RedisTemplate<Object, Object>**
    - Available if you need generic object storage
    - Configurable serializers

4. **Caching Support**
    - Redis cache manager auto-configured
    - Works with Spring's `@Cacheable` annotations

## Benefits

### 1. **Simpler Code**

- No manual template creation
- No factory wiring in application code
- Less boilerplate

### 2. **Better Maintainability**

- Standard Spring Boot property naming
- Less custom code to maintain
- Follows Spring Boot best practices

### 3. **Improved Performance**

- Lettuce client (async/reactive capable)
- Connection pooling by default
- Better resource management

### 4. **Future-Proof**

- Aligned with Spring Boot conventions
- Easier to upgrade Spring Boot versions
- Compatible with Spring Boot monitoring/actuators

## Configuration Properties Reference

### Core Redis Properties

| Property                        | Description          | Default     |
|---------------------------------|----------------------|-------------|
| `spring.data.redis.host`        | Redis server host    | `localhost` |
| `spring.data.redis.port`        | Redis server port    | `6379`      |
| `spring.data.redis.password`    | Redis password       | -           |
| `spring.data.redis.database`    | Redis database index | `0`         |
| `spring.data.redis.timeout`     | Connection timeout   | `2000ms`    |
| `spring.data.redis.ssl.enabled` | Enable SSL           | `false`     |

### Connection Pool Properties (Lettuce)

| Property                                    | Description          | Default |
|---------------------------------------------|----------------------|---------|
| `spring.data.redis.lettuce.pool.enabled`    | Enable pooling       | `true`  |
| `spring.data.redis.lettuce.pool.max-active` | Max connections      | `8`     |
| `spring.data.redis.lettuce.pool.max-idle`   | Max idle connections | `8`     |
| `spring.data.redis.lettuce.pool.min-idle`   | Min idle connections | `0`     |

### Sentinel Properties (for High Availability)

| Property                            | Description    | Example                   |
|-------------------------------------|----------------|---------------------------|
| `spring.data.redis.sentinel.master` | Master name    | `master`                  |
| `spring.data.redis.sentinel.nodes`  | Sentinel nodes | `host1:26379,host2:26379` |

### Cache Properties

| Property                               | Description       | Default |
|----------------------------------------|-------------------|---------|
| `spring.cache.redis.time-to-live`      | Cache TTL         | -       |
| `spring.cache.redis.cache-null-values` | Cache null values | `true`  |
| `spring.cache.redis.use-key-prefix`    | Use key prefix    | `true`  |

## No Configuration Class Needed!

Unlike the old approach, you **don't need any Redis configuration class** with Spring Boot:

- ❌ **No need for** `@Configuration` class with `@Bean` methods
- ❌ **No need for** manual `RedisConnectionFactory` bean
- ❌ **No need for** manual `RedisTemplate` bean
- ✅ **Just autowire** `StringRedisTemplate` or `RedisTemplate` directly

## Testing

Redis is configured for all test environments:

- ✅ Unit tests can use embedded Redis (or testcontainers)
- ✅ Integration tests use configured Redis instance
- ✅ Properties override works seamlessly

## Usage Example

Simply autowire the template wherever you need it:

```java

@Service
public class MyService {

  @Autowired
  private StringRedisTemplate redisTemplate;

  public void saveValue(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  public String getValue(String key) {
    return redisTemplate.opsForValue().get(key);
  }
}
```

No configuration needed - Spring Boot handles everything!

## Advanced Features (Available if Needed)

### 1. Custom Serializers

If you need custom serialization, create a single `@Bean`:

```java

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, MyObject> myObjectRedisTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, MyObject> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(MyObject.class));
    return template;
  }
}
```

### 2. Redis Cache Manager

Spring Boot autoconfigures this too:

```java

@Service
public class MyService {

  @Cacheable(value = "myCache", key = "#id")
  public MyObject findById(String id) {
    // Expensive operation - result will be cached in Redis
    return expensiveOperation(id);
  }
}
```

### 3. Redis Repositories

Spring Data Redis repositories work out of the box:

```java

@RedisHash("persons")
public class Person {

  @Id
  private String id;
  private String name;
}

public interface PersonRepository extends CrudRepository<Person, String> {

}
```

## Migration Checklist

- [x] Remove manual `StringRedisTemplate` creation from `LogStore`
- [x] Autowire Spring Boot's auto-configured `StringRedisTemplate`
- [x] Remove `RedisConnectionFactory` dependency from `LogStore`
- [x] Verify properties use Spring Boot conventions (`spring.data.redis.*`)
- [x] Update documentation in property files
- [x] Verify `spring-boot-starter-data-redis` dependency exists
- [x] Test Redis connectivity

## Next Steps (Optional)

1. **Add Redis Health Check**
   ```properties
   management.health.redis.enabled=true
   ```

2. **Add Connection Pool Tuning** (if needed)
   ```properties
   spring.data.redis.lettuce.pool.max-active=20
   spring.data.redis.lettuce.pool.max-idle=10
   spring.data.redis.lettuce.pool.min-idle=5
   ```

3. **Configure Sentinel for Production** (High Availability)
   ```properties
   spring.data.redis.sentinel.master=mymaster
   spring.data.redis.sentinel.nodes=sentinel1:26379,sentinel2:26379,sentinel3:26379
   ```

4. **Add Redis Metrics**
   ```properties
   management.metrics.enable.redis=true
   ```

## References

- [Spring Boot Redis Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.nosql.redis)
- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Lettuce (Redis Client) Documentation](https://lettuce.io/)

## Migration Date

2025-12-09

## Status

✅ **COMPLETE** - Redis now fully uses Spring Boot autoconfiguration

