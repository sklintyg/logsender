# Requirements for Migrating logsender from Spring Framework to Spring Boot

## Overview
Migrate the logsender application from Spring Framework with XML configuration and Apache Camel to Spring Boot 3.5.8, following modern Spring patterns, maintaining Apache Camel for routing, and implementing structured ECS logging.

---

## 1. Core Framework

### 1.1 Spring Boot Version
- **Spring Boot 3.x** (latest stable)
- **Java 21** (upgrade from current version if needed)
- **Gradle** for build management
- **Use intyg-bom** for dependency management to ensure consistent versions across Inera applications
- **Use Spring Boot BOM** where possible for Spring Boot dependencies

### 1.2 Application Structure
- Replace XML-based configuration (`applicationContext.xml`, `camel-context.xml`) with Java-based `@Configuration` classes
- Use `@SpringBootApplication` as main entry point
- Automatic component scanning from base package `se.inera.intyg.logsender`
- Replace WAR packaging with executable JAR (embedded Tomcat)

---

## 2. Dependency Injection

### 2.1 Pattern
- Use **constructor injection** via Lombok's `@RequiredArgsConstructor`
- All dependencies as `final` fields for immutability
- Replace any XML-based bean definitions with `@Component`, `@Service`, `@Configuration` annotations
- **Drop common and infra imports**: Remove dependencies on common and infra libraries, use Spring Boot autoconfig instead

### 2.2 Component Scanning
- Use `@SpringBootApplication` as main entry point
- Automatic component scanning from base package
- Use `@ComponentScan` only if needed for specific packages

---

## 3. Apache Camel Integration

### 3.1 Camel Spring Boot Integration
- **Keep Apache Camel**: Maintain Apache Camel for routing and message handling
- Use `camel-spring-boot-starter` for Spring Boot integration
- Replace XML routes (`camel-context.xml`) with **Java DSL** (`RouteBuilder` classes)
- Use Spring Boot's auto-configuration for Camel

### 3.2 Route Configuration
- Create `@Component` classes extending `RouteBuilder` for Camel routes
- Use Java DSL for route definitions instead of XML
- Configure Camel properties in `application.properties`
- Maintain existing route logic and transformations

### 3.3 Camel Components
- Review and update Camel component dependencies to Spring Boot compatible versions
- Use Camel Spring Boot starters where available
- Ensure all Camel processors, converters, and services are Spring-managed beans

---

## 4. Configuration Management

### 4.1 Properties
- Use **application.properties** format (not YAML)
- Externalize environment-specific values with placeholders `${property.name}`
- Support for `spring.config.additional-location` for environment overrides
- Migrate properties from XML configurations to application.properties

### 4.2 Profile Management
- `dev` - Development environment
- `test` - Test environment
- Profile-specific configuration files: `application-{profile}.properties`
- Use `@Profile` annotations on `@Configuration` classes for environment-specific beans

**Example:**
```java
@Profile({"dev"})
@Configuration
public class DevConfig {
  // Configuration for dev environment
}
```

### 4.3 Configuration Injection
- Use `@Value` for simple property injection
- Use `@ConfigurationProperties` for grouped configurations
- Replace XML property placeholders with Spring Boot property injection

### 4.4 Custom Bean Configuration
Create `@Configuration` classes for custom beans:
- **ApplicationConfig** - Core beans (ObjectMapper, MessageSource, etc.)
- **CamelConfig** - Camel-specific configurations if needed
- Separate configuration classes by domain

---

## 5. Logging and Observability

### 5.1 Logging
- **Structured ECS logging**: Implement Elastic Common Schema (ECS) formatted logging
- Use **Logback** with ECS encoder for JSON structured logs
- Configure logging in `logback-spring.xml`
- Use existing logging patterns from `logback-spring-base.xml` as reference

### 5.2 MDC Context
- Maintain MDC (session ID, trace ID) via `Filter` implementation
- Implement as Spring `@Component` implementing `Filter` interface
- Set MDC keys at filter level for tracing
- Clear MDC context in finally block to prevent leaks

**Example:**
```java
@Component
public class MdcFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
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
- **Spring Boot Actuator** for health checks and metrics
- Enable endpoints: `/actuator/health`, `/actuator/metrics`, `/actuator/info`
- Configure management endpoints appropriately

### 5.4 Performance Logging
- Implement performance logging via **Spring AOP** (`@Aspect`) if needed
- Use custom annotations for method-level tracking
- Log Camel route performance and message processing

---

## 6. Security

### 6.1 Current State
- Review current security implementation
- Maintain existing security patterns if applicable
- Consider adding security headers via filter if needed

### 6.2 Considerations
- Input validation for defense in depth
- Maintain audit logging via MDC and performance logging
- Secure configuration of external endpoints

---

## 7. Testing Strategy

### 7.1 Unit Tests
- Use **JUnit 5** (Jupiter)
- **Mockito** for mocking dependencies
- Test services with mocked dependencies
- Test Camel routes with `CamelTestSupport` or Spring Boot Camel test support

### 7.2 Integration Tests
- `@SpringBootTest` for full application context
- Test Camel routes with embedded context
- Use profile-specific configurations for tests
- Maintain existing integration test patterns

### 7.3 Test Data Management
- Use Gradle `java-test-fixtures` plugin for shared test data if multi-module
- Create reusable test fixtures in dedicated source set
- Bootstrap data for dev profile using `@PostConstruct` with `@Profile("dev")`

---

## 8. Error Handling

### 8.1 Global Exception Handler
Implement centralized error handling using `@ControllerAdvice` (if REST endpoints exist):

```java
@ControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(CustomServiceException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ErrorResponse handleServiceException(CustomServiceException e) {
    LOG.warn("Service exception: {} - {}", e.getErrorCode(), e.getMessage());
    return new ErrorResponse(e.getErrorCode(), e.getMessage());
  }
}
```

### 8.2 Camel Error Handling
- Use Camel's `onException` and `errorHandler` in routes
- Implement dead letter channel for failed messages
- Log errors with appropriate context

### 8.3 Custom Exceptions
- Create custom exception classes with error codes
- Use enumerated error codes for handling
- Include context in exception messages

---

## 9. External Integrations

### 9.1 REST Client Configuration
If external integrations exist:
- Use Spring 6's `RestClient` (not legacy `RestTemplate`)
- Configure as `@Bean` in `@Configuration` class
- Use integration contracts (interfaces) separate from implementations

```java
@Configuration
public class RestClientConfig {
  @Bean
  public RestClient externalServiceRestClient(@Value("${external.service.url}") String baseUrl) {
    return RestClient.create(baseUrl);
  }
}
```

### 9.2 Camel HTTP Components
- Use Camel HTTP components for external service calls if appropriate
- Configure connection pools and timeouts
- Handle retries and circuit breaking

---

## 10. Packaging and Deployment

### 10.1 Packaging
- Change from **WAR** to **JAR** (embedded Tomcat)
- Use `spring-boot-gradle-plugin`
- Executable JAR with embedded servlet container

### 10.2 Docker
- Update Dockerfile to run Spring Boot JAR
- Remove application server-specific configurations
- Simplified container setup (no external Tomcat needed)

### 10.3 Deployment Configuration
- Externalize configuration via environment variables or config files
- Support for different deployment environments (dev, test, prod)

---

## 11. Migration Steps (High Level)

1. **Add Spring Boot dependencies** to `build.gradle`
2. **Create `@SpringBootApplication` main class**
3. **Migrate XML configuration to Java**:
   - Convert `applicationContext.xml` to `@Configuration` classes
   - Convert `camel-context.xml` to Java DSL `RouteBuilder` classes
4. **Update Camel integration**:
   - Add `camel-spring-boot-starter`
   - Convert routes to Java DSL
   - Configure Camel in `application.properties`
5. **Remove common and infra dependencies**:
   - Replace with Spring Boot equivalents
   - Use Spring Boot auto-configuration
6. **Implement structured ECS logging**:
   - Configure Logback with ECS encoder
   - Update `logback-spring.xml`
7. **Add `@Configuration` classes** for custom beans
8. **Update filters** to Spring `@Component` filters (MDC filter)
9. **Add Actuator** endpoints
10. **Update tests** to use Spring Boot testing annotations
11. **Update Gradle build** for JAR packaging
12. **Update Docker/deployment** configuration for JAR execution

---

## 12. Dependencies to Add

### 12.1 Spring Boot Starters
- `spring-boot-starter-web` (if REST endpoints exist)
- `spring-boot-starter-actuator` (Health/metrics)
- `spring-boot-starter-test` (Testing)

### 12.2 Apache Camel Spring Boot
- `camel-spring-boot-starter`
- Camel component starters as needed (e.g., `camel-http-starter`, `camel-jms-starter`)

### 12.3 Logging
- `logstash-logback-encoder` (for ECS structured logging)
- `logback-classic` (likely already in use)

### 12.4 Additional
- `spring-boot-starter-aop` (if AOP needed for logging)
- `lombok` (already in use)

### 12.5 BOM Usage
> **Dependency Management:**
> - Use the **intyg-bom** for Inera-specific dependency versions
> - Use the [Spring Boot BOM](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html#dependency-versions) for Spring Boot dependencies
> - Use Camel Spring Boot BOM for Camel dependencies

### 12.6 Remove
- Common and infra library dependencies
- Standalone Tomcat/servlet dependencies
- XML-based Spring dependencies not needed with Spring Boot

---

## 13. Best Practices to Follow

- ✅ Constructor injection with `@RequiredArgsConstructor`
- ✅ Immutability: `final` fields
- ✅ **Java config over XML**: No XML configuration files
- ✅ **Google Java Style Guide** for code formatting
- ✅ No comments unless necessary (code should be self-documenting)
- ✅ Use streams over loops where applicable
- ✅ Descriptive method and variable names
- ✅ Separation of concerns: clear layer separation
- ✅ Use `Optional` for nullable returns
- ✅ Externalize all configuration
- ✅ Profile-based configuration management
- ✅ MDC context propagation via Filter
- ✅ **Use Spring Boot autoconfig wherever possible**
- ✅ Camel routes in Java DSL, not XML

---

## 14. Non-Goals

- ❌ No reactive programming (WebFlux) - keep traditional servlet model
- ❌ No major API contract changes - maintain backward compatibility
- ❌ No changes to core Camel routing logic - just migration to Java DSL
- ❌ No distributed tracing implementation initially (future enhancement)
- ❌ No caching implementation initially (future enhancement)

---

**Document Version:** 1.0  
**Last Updated:** December 8, 2025  
**Migration Target:** Spring Boot 3.x, Java 21  
**Current State:** Spring Framework with XML configuration and Apache Camel

