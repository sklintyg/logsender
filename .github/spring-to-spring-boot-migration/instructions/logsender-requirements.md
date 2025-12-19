# Requirements for Migrating logsender from Spring Framework to Spring Boot

## Overview
Migrate the logsender application from Spring Framework with XML configuration and Apache Camel to Spring Boot 3.x, following modern Spring patterns, maintaining Apache Camel for routing, and implementing structured ECS logging.

---

## 1. Core Framework

### 1.1 Spring Boot Version
- **Spring Boot 3.5.8**
- **Java 21** (upgrade from current Java version)
- **Gradle** for build management
- **Use intyg-bom** for dependency management
- **Use Spring Boot BOM** for Spring dependencies
- **Use Camel Spring Boot BOM** for Camel dependencies

### 1.2 Application Structure
- Replace XML configuration (`applicationContext.xml`, `camel-context.xml`) with Java `@Configuration` classes
- Create `@SpringBootApplication` main class in `se.inera.intyg.logsender`
- Automatic component scanning from base package
- Change from WAR to executable JAR with embedded Tomcat

---

## 2. Dependency Injection

### 2.1 Pattern (from PPS inspiration)
- **Constructor injection exclusively** via Lombok's `@RequiredArgsConstructor`
- All dependencies as `final` fields for immutability
- No field injection (`@Autowired` on fields)
- **Drop common and infra imports** - use Spring Boot autoconfiguration instead

### 2.2 Component Scanning
- Use `@SpringBootApplication` on main class (no explicit `@ComponentScan` needed)
- Automatic scanning from `se.inera.intyg.logsender`

**Example:**
```java
@SpringBootApplication
public class LogsenderApplication {
  public static void main(String[] args) {
    SpringApplication.run(LogsenderApplication.class, args);
  }
}
```

---

## 3. Apache Camel Integration

### 3.1 Camel Spring Boot Integration
- **Keep Apache Camel** for routing and message handling
- Use `camel-spring-boot-starter` for Spring Boot integration
- Replace XML routes (`camel-context.xml`) with **Java DSL** (`RouteBuilder` classes)
- Let Spring Boot autoconfigure Camel

### 3.2 Route Configuration
- Create `@Component` classes extending `RouteBuilder`
- Use Java DSL for all route definitions
- Configure Camel properties in `application.properties` (e.g., `camel.springboot.main-run-controller=true`)
- Maintain existing route logic, processors, and converters

**Example:**
```java
@Component
public class LogMessageRoute extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from("direct:logMessage")
      .process(exchange -> {
        // existing logic
      })
      .to("endpoint");
  }
}
```

### 3.3 Camel Components
- Update Camel component dependencies to Spring Boot starters
- Use Camel Spring Boot starters where available (e.g., `camel-http-starter`)
- Ensure all processors, converters, and services are Spring-managed beans with `@Component` or `@Service`

---

## 4. Configuration Management

### 4.1 Properties (from PPS inspiration)
- Use **application.properties** format (not YAML)
- Externalize environment-specific values: `${property.name}`
- Support `spring.config.additional-location` for environment overrides
- Migrate all XML properties to `application.properties`

### 4.2 Profile Management
- `dev` - Development environment with bootstrap data
- `test` - Test environment
- Profile-specific files: `application-dev.properties`, `application-test.properties`
- Use `@Profile` on configuration classes

**Example:**
```java
@Profile("dev")
@Configuration
public class DevConfig {
  // Dev-specific beans
}
```

### 4.3 Configuration Injection
- **Prefer `@ConfigurationProperties`** for grouped settings over multiple `@Value` annotations
- Use `@Value` only for simple single properties
- Enable validation with `@Validated` on configuration classes

**Example:**
```java
@ConfigurationProperties(prefix = "logsender")
@Validated
@Data
public class LogsenderProperties {
  @NotBlank
  private String endpoint;
  
  @Positive
  private int timeout;
}
```

### 4.4 Custom Bean Configuration
Create `@Configuration` classes:
- **ApplicationConfig** - Core beans (ObjectMapper, etc.)
- **CamelConfig** - Camel-specific configuration if needed
- Separate by domain for clarity

---

## 5. Logging and Observability

### 5.1 Structured ECS Logging
- **Elastic Common Schema (ECS)** formatted logging
- Use **Logback** with `logstash-logback-encoder` for JSON output
- Configure in `logback-spring.xml`
- Use `logback-spring-base.xml` as reference

**Configuration:**
```properties
logging.structured.format.console=ecs
```

### 5.2 MDC Context (from PPS inspiration)
- Implement MDC filter as Spring `@Component` implementing `Filter`
- Set MDC keys: session ID, trace ID
- Clear MDC in finally block to prevent leaks

**Example:**
```java
@Component
public class MdcLoggingFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
      throws IOException, ServletException {
    try {
      MDC.put(SESSION_ID_KEY, sessionId);
      MDC.put(TRACE_ID_KEY, traceId);
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
```

### 5.3 Actuator
- Add `spring-boot-starter-actuator`
- Enable health, metrics, info endpoints
- Configure management endpoints

**Configuration:**
```properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=when-authorized
```

### 5.4 Performance Logging (from PPS inspiration)
- Implement via **Spring AOP** (`@Aspect`)
- Custom annotation for declarative logging
- Log Camel route performance

**Example:**
```java
@Component
@Aspect
public class PerformanceLoggingAspect {
  @Around("@annotation(performanceLogging)")
  public Object logPerformance(ProceedingJoinPoint joinPoint, 
                               PerformanceLogging performanceLogging) throws Throwable {
    long start = System.currentTimeMillis();
    try {
      return joinPoint.proceed();
    } finally {
      long duration = System.currentTimeMillis() - start;
      log.info("Method {} took {} ms", joinPoint.getSignature(), duration);
    }
  }
}
```

---

## 6. Error Handling

### 6.1 Camel Error Handling
- Use Camel's `onException` and `errorHandler` in routes
- Implement dead letter channel for failed messages
- Log errors with MDC context

**Example:**
```java
@Override
public void configure() throws Exception {
  errorHandler(deadLetterChannel("direct:errorQueue")
    .maximumRedeliveries(3)
    .redeliveryDelay(5000));
    
  onException(Exception.class)
    .log("Error processing message: ${exception.message}");
}
```

### 6.2 Custom Exceptions
- Create custom exception hierarchy
- Include error codes for categorization
- Provide meaningful context

---

## 7. Testing Strategy

### 7.1 Unit Tests (from PPS inspiration)
- **JUnit 5** (Jupiter)
- **Mockito** with `@ExtendWith(MockitoExtension.class)`
- `@Mock` and `@InjectMocks` for dependency injection
- Pure unit tests without Spring context

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class LogServiceTest {
  @Mock
  private LogRepository logRepository;
  
  @InjectMocks
  private LogService logService;
  
  @Test
  void shouldProcessLog() {
    // Test logic
  }
}
```

### 7.2 Integration Tests (from PPS inspiration)
- `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
- Full Spring context with Camel routes
- Profile-specific configurations
- Consider **Testcontainers** if external dependencies exist

**Example:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class LogMessageRouteIT {
  @Autowired
  private CamelContext camelContext;
  
  @Test
  void shouldProcessMessage() {
    // Integration test
  }
}
```

### 7.3 Camel Route Testing
- Use Spring Boot Camel test support
- Test routes with embedded context
- Mock external endpoints

### 7.4 Test Data Management
- Use Gradle `java-test-fixtures` plugin if multi-module
- Bootstrap data for dev profile using `@PostConstruct` with `@Profile("dev")`

---

## 8. Packaging and Deployment

### 8.1 Packaging
- Change from **WAR** to **JAR**
- Use `spring-boot-gradle-plugin`
- Executable JAR with embedded Tomcat

**Gradle configuration:**
```gradle
plugins {
  id 'org.springframework.boot' version '3.5.8'
  id 'io.spring.dependency-management' version '1.1.7'
  id 'java'
}

bootJar {
  enabled = true
}

jar {
  enabled = false
}
```

### 8.2 Docker
- Update Dockerfile to run Spring Boot JAR
- Remove Tomcat/application server configurations
- Simplified container setup

**Dockerfile example:**
```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/logsender.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 9. Migration Steps (High Level)

1. **Update Gradle dependencies** - Add Spring Boot, Camel Boot starters, remove old dependencies
2. **Create `@SpringBootApplication` main class**
3. **Migrate `applicationContext.xml` to Java `@Configuration` classes**
4. **Convert `camel-context.xml` routes to Java DSL `RouteBuilder` classes**
5. **Remove common and infra dependencies** - Replace with Spring Boot equivalents
6. **Configure structured ECS logging** - Update `logback-spring.xml`
7. **Implement MDC filter** as Spring `@Component`
8. **Add Spring Boot Actuator**
9. **Update tests** to Spring Boot test annotations
10. **Change packaging** from WAR to JAR
11. **Update Dockerfile** for JAR execution

---

## 10. Dependencies

### 10.1 Add
- `spring-boot-starter-web` (if web/REST needed)
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`
- `spring-boot-starter-aop` (for performance logging)
- `camel-spring-boot-starter`
- Camel component starters (e.g., `camel-http-starter`, `camel-jms-starter`)
- `logstash-logback-encoder` (ECS logging)
- `lombok` (if not already present)

### 10.2 Remove
- Common and infra library dependencies
- Standalone Tomcat/servlet dependencies
- Spring Framework XML dependencies

### 10.3 BOM Usage
```gradle
dependencyManagement {
  imports {
    mavenBom "se.inera.intyg:intyg-bom:${intygBomVersion}"
    mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
  }
}
```

---

## 11. Best Practices

- ✅ **Constructor injection** with `@RequiredArgsConstructor`
- ✅ **Immutability**: `final` fields
- ✅ **Java config over XML**: No XML configuration
- ✅ **Google Java Style Guide** for formatting
- ✅ **Self-documenting code**: Minimize comments
- ✅ **Streams over loops** where applicable
- ✅ **Descriptive naming**: Clear method and variable names
- ✅ **Separation of concerns**: Clear layer boundaries
- ✅ **Optional for nullable** returns
- ✅ **Externalized configuration**
- ✅ **MDC context propagation**
- ✅ **Spring Boot autoconfiguration** wherever possible
- ✅ **Camel routes in Java DSL**

---

## 12. Non-Goals

- ❌ No reactive programming (WebFlux)
- ❌ No major API contract changes
- ❌ No changes to core Camel routing logic
- ❌ No distributed tracing initially
- ❌ No caching initially
- ❌ No database layer (unless already present)

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-08  
**Migration Target:** Spring Boot 3.5.8, Java 21, Apache Camel Spring Boot  
**Current State:** Spring Framework with XML configuration and Apache Camel  
**Inspiration Source:** private-practitioner-service (PPS) Spring Boot patterns

