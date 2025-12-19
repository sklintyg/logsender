# Requirements for Migrating hjmtj-external-api from Wildfly to Spring Boot

## Overview
Migrate the hjmtj-external-api application from Wildfly/Jakarta EE (JAX-RS) to Spring Boot 3.x, following modern Spring patterns and maintaining existing functionality.

---

## 1. Core Framework

### 1.1 Spring Boot Version
- **Spring Boot 3.5.6** (latest stable)
- **Java 21** (maintain current version)
- **Gradle** for build management
- **Use Spring Boot BOM** for dependency management where possible to ensure consistent and compatible versions of Spring Boot and related libraries

### 1.2 Web Layer
- Replace JAX-RS (`@ApplicationPath`, `@Path`) with **Spring MVC** (`@RestController`, `@RequestMapping`)
- Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` for HTTP methods
- Replace JAX-RS `Response` with Spring `ResponseEntity` where needed
- Maintain `/v1` API path prefix

---

## 2. Dependency Injection

### 2.1 Pattern
- Use **constructor injection** via Lombok's `@RequiredArgsConstructor`
- All dependencies as `final` fields for immutability
- Replace any CDI annotations (`@Inject`, `@ApplicationScoped`) with Spring annotations (`@Service`, `@Component`, `@Configuration`)

### 2.2 Component Scanning
- Use `@SpringBootApplication` as main entry point
- Automatic component scanning from base package `se.inera.hjmtj.externalapi`

---

## 3. Data Access

### 3.1 JPA/Hibernate Configuration
- **Spring Data JPA** with Hibernate provider
- Use `spring-boot-starter-data-jpa`
- MySQL database (production)
- H2 for testing (optional)
- **Liquibase** for database schema versioning and migrations

### 3.2 Repository Pattern (Three-Layer Architecture)
Follow the three-layer repository pattern for clean architecture:

1. **JPA Repository Layer** (Data layer)
    - Create `@Repository` interfaces extending `JpaRepository`
    - Use `@Query` annotations for custom JPQL queries
    - Example: `CertificateEntityRepository`

2. **Domain Repository Layer** (Abstraction layer)
    - Create `@Repository` classes that wrap JPA repositories
    - Convert between JPA entities and domain models using converter classes
    - Return domain models (not entities) to service layer
    - Example: `CertificateRepository`

3. **Service Layer** (Business logic)
    - Services interact only with domain repositories
    - Never directly access JPA repositories from services
    - Work with domain models, not entities

### 3.3 Converter Pattern
Implement converters at each architectural boundary:

- **Entity Converters** (Infrastructure layer): JPA Entity ↔ Domain Model
    - Example: `CertificateEntityConverter`
- **DTO Converters** (Application layer): Domain Model ↔ API DTO
    - Example: `CertificateDTOConverter`
- All converters as `@Component` for dependency injection
- Keep conversion logic isolated and testable

### 3.4 Transaction Management
- Use `@Transactional` at service layer (not repositories or controllers)
- Explicit transaction boundaries for business operations
- Use `@Transactional(readOnly = true)` for query-only methods (performance optimization)
- Transaction rollback automatic on RuntimeException

### 3.5 Database Configuration
```properties
spring.datasource.url=jdbc:mysql://${database.server}:${database.port}/${database.name}
spring.datasource.username=${database.username}
spring.datasource.password=${database.password}
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.change-log=classpath:changelog/changelog.xml
```

---

## 4. Configuration Management

### 4.1 Properties
- Use **application.properties** format (not YAML)
- Externalize environment-specific values with placeholders `${property.name}`
- Support for `spring.config.additional-location` for environment overrides
- Use Unicode escapes for special characters if needed (e.g., `\u00E4` for Swedish characters)

### 4.2 Profile Management
- `dev` - Development environment
- `integration-test` - Integration tests
- Profile-specific configuration files: `application-{profile}.properties`
- Use `@Profile` annotations on `@Configuration` classes for environment-specific beans

**Example:**
```java
@Profile({"dev"})
@Configuration
public class DevBootstrapConfig {
  // Configuration for dev environment
}
```

### 4.3 Configuration Injection
- Use `@Value` for simple property injection
- Consider `@ConfigurationProperties` for grouped configurations (future improvement)

### 4.4 Custom Bean Configuration
Create `@Configuration` classes for custom beans:
- **ApplicationConfig** - Core beans (ObjectMapper, MessageSource)
- **Custom ObjectMapper** - Configure JSON serialization with `@Primary` annotation
- Separate configuration classes by domain (e.g., MailConfig, JobConfig)

---

## 5. Logging and Observability

### 5.1 Logging
- **Spring Boot structured logging** with Logback
- ECS (Elastic Common Schema) format for production
- Use existing logging module with Spring integration

### 5.2 MDC Context
- Maintain MDC (session ID, trace ID) via `Filter` implementation
- Implement as Spring `@Component` implementing `Filter` interface
- Set MDC keys at filter level: `SESSION_ID_KEY`, `TRACE_ID_KEY`
- Clear MDC context in finally block to prevent leaks

**Example:**
```java
@Component
public class MdcServletFilter implements Filter {
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
- Configure management port if needed

### 5.4 Performance Logging
- Implement performance logging via **Spring AOP** (`@Aspect`)
- Use `@PerformanceLogging` custom annotation for method-level tracking
- Log event actions and types for audit trail

---

## 6. Security

### 6.1 Current State
- **No Spring Security required** - internal API behind gateway
- No authentication/authorization at application level
- API Gateway handles security concerns

### 6.2 Considerations
- Add security headers via filter if needed
- Input validation for defense in depth
- Maintain audit logging via MDC and performance logging

---

## 7. Testing Strategy

### 7.1 Unit Tests
- Use **JUnit 5** (Jupiter)
- **Mockito** for mocking dependencies
- Test services with mocked repositories

### 7.2 Integration Tests
- `@SpringBootTest` for full application context
- **Testcontainers** for MySQL (if complex queries need testing)
- H2 in-memory database for simpler integration tests
- `@WebMvcTest` for controller testing with mock services

### 7.3 Repository Tests
- `@DataJpaTest` for repository layer testing
- Use test-specific profiles
- Consider Testcontainers for testing against real MySQL

### 7.4 Test Data Management
- Use Gradle `java-test-fixtures` plugin for shared test data
- Create reusable test fixtures in dedicated source set
- Share test data classes across modules
- Bootstrap data for dev profile using `@PostConstruct` with `@Profile("dev")`

---

## 8. Error Handling

### 8.1 Global Exception Handler
Implement centralized error handling using `@ControllerAdvice`:

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
  
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ErrorResponse handleUnhandledException(RuntimeException e) {
    LOG.error("Unhandled exception", e);
    return new ErrorResponse(ErrorCode.UNKNOWN, "Internal server error");
  }
}
```

### 8.2 Custom Exceptions
- Create custom exception classes with error codes
- Use enumerated error codes for client handling
- Include context in exception messages
- Don't expose stack traces to clients

### 8.3 Error Response Structure
Define consistent error response DTOs:
```java
@Value
@Builder
public class ErrorResponse {
  ErrorCode errorCode;
  String message;
}
```

---

## 9. External Integrations

### 9.1 REST Client Configuration
If external integrations exist:
- Use Spring 6's `RestClient` (not legacy `RestTemplate`)
- Configure as `@Bean` in `@Configuration` class
- Use integration contracts (interfaces) separate from implementations
- Example naming: `ExternalServiceRestClient`

```java
@Configuration
public class RestClientConfig {
  @Bean
  public RestClient externalServiceRestClient(@Value("${external.service.url}") String baseUrl) {
    return RestClient.create(baseUrl);
  }
}
```

### 9.2 Integration Module Structure
- Create separate modules for external integrations if complex
- Define integration contracts in separate API module
- Implement REST clients in implementation module

---

## 10. Module Structure

### 10.1 Current Modules
- `api` - API contracts/DTOs
- `app` - Application layer (controllers, configuration)
- `model` - Domain entities
- `property` - Configuration properties
- `logging` - Logging utilities

### 10.2 Migration Approach
- Maintain multi-module structure
- Migrate each module's dependencies to Spring equivalents
- Keep clear separation between layers

---

## 11. Packaging and Deployment

### 11.1 Packaging
- Change from **WAR** to **JAR** (embedded Tomcat)
- Use `spring-boot-gradle-plugin`
- Executable JAR with embedded servlet container

### 11.2 Docker
- Update Dockerfile to run Spring Boot JAR
- Remove Wildfly-specific configurations
- Simplified container setup (no application server needed)

---

## 12. Migration Steps (High Level)

1. **Add Spring Boot dependencies** to root and module `build.gradle` files
2. **Create `@SpringBootApplication` main class** replacing JAX-RS configuration
3. **Set up Liquibase** for database schema management
4. **Convert controllers**: JAX-RS → Spring MVC annotations
5. **Implement three-layer repository pattern**:
    - Create JPA repositories extending `JpaRepository`
    - Create domain repositories wrapping JPA repositories
    - Create converter classes for Entity ↔ Domain ↔ DTO
6. **Convert filters** to Spring `@Component` filters (MDC filter)
7. **Update configuration**: `persistence.xml` → `application.properties`
8. **Add `@Configuration` classes** for custom beans (ObjectMapper, etc.)
9. **Implement global exception handler** with `@ControllerAdvice`
10. **Add Actuator** endpoints
11. **Implement AOP** for performance logging
12. **Update tests** to use Spring testing annotations
13. **Add test fixtures** for shared test data
14. **Update Docker/deployment** configuration for JAR execution

---

## 13. Dependencies to Add

### 13.1 Spring Boot Starters
- `spring-boot-starter-web` (Spring MVC)
- `spring-boot-starter-data-jpa` (JPA/Hibernate)
- `spring-boot-starter-actuator` (Health/metrics)
- `spring-boot-starter-test` (Testing)

> **Dependency Management:**
> - Use the [Spring Boot BOM](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html#dependency-versions) in your Gradle build files to manage Spring Boot and related dependencies. This ensures all Spring modules and starters use compatible versions and simplifies version management across modules.

### 13.2 Additional
- `spring-boot-starter-aop` (AOP for logging)
- `liquibase-core` (Database migrations)
- `lombok` (already in use)
- `mysql-connector-j` (already in use)
- `logback-classic` (already in use)

### 13.3 Testing
- `testcontainers` (Optional: for integration tests with real MySQL)
- Gradle `java-test-fixtures` plugin for shared test data

### 13.4 Remove
- Jakarta EE API dependencies (provided by Wildfly)
- JAX-RS implementations
- CDI implementations

---

## 14. Best Practices to Follow

- ✅ Constructor injection with `@RequiredArgsConstructor`
- ✅ Immutability: `final` fields, immutable DTOs
- ✅ Three-layer repository pattern: JPA Repository → Domain Repository → Service
- ✅ Converter classes at each boundary: Entity ↔ Domain ↔ DTO
- ✅ No comments unless necessary (code should be self-documenting)
- ✅ Use streams over loops where applicable
- ✅ `@PerformanceLogging` for significant operations
- ✅ `@Transactional` at service layer with `readOnly = true` for queries
- ✅ Descriptive method and variable names
- ✅ Separation of concerns: Controller → Service → Repository layers
- ✅ Use `Optional` for nullable returns
- ✅ Externalize all configuration
- ✅ Profile-based configuration management
- ✅ Global exception handler with `@ControllerAdvice`
- ✅ MDC context propagation via Filter

---

## 15. Non-Goals

- ❌ No reactive programming (WebFlux) - keep traditional servlet model
- ❌ No Spring Security implementation - remains internal API
- ❌ No caching implementation initially (future enhancement)
- ❌ No distributed tracing (unless required later)
- ❌ No major API contract changes - maintain backward compatibility

---

**Document Version:** 2.0  
**Last Updated:** December 1, 2025  
**Migration Target:** Spring Boot 3.5.6, Java 21  
**Current State:** Wildfly/Jakarta EE with JAX-RS