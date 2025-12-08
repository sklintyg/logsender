# Private Practitioner Service - Spring Design Choices Analysis

## Executive Summary

This document provides a comprehensive analysis of Spring Framework and Spring Boot design choices in the Private Practitioner Service (PPS). The service is built using **Spring Boot 3.x** with **Java 21**, following a clean architecture approach with separation between application, domain, and infrastructure layers.

---

## 1. Dependency Injection Strategy

### 1.1 Constructor Injection (Primary Pattern)

**Implementation:**
- The application **exclusively uses constructor injection** via Lombok's `@RequiredArgsConstructor`
- All dependencies are marked as `final` fields
- No field injection (`@Autowired` on fields) is used

**Examples:**
```java
@Service
@RequiredArgsConstructor
public class PrivatePractitionerService {
  private final PrivatePractitionerRepository privatePractitionerRepository;
  private final PrivatePractitionerConverter privatePractitionerConverter;
  private final UpdatePrivatePractitionerFromPUService updatePrivatePractitionerFromPUService;
}

@RestController
@RequestMapping("/internalapi/privatepractitioner")
@RequiredArgsConstructor
public class PrivatePractitionerController {
  private final CreateRegistrationService createRegistrationService;
  private final UpdatePrivatePractitionerService updatePrivatePractitionerService;
  // ... other services
}
```

**Benefits:**
- ✅ **Immutability**: Final fields ensure dependencies cannot be changed after construction
- ✅ **Testability**: Easy to create instances with mock dependencies in tests (see `PrivatePractitionerControllerTest`)
- ✅ **Clarity**: All dependencies are explicitly visible in constructor
- ✅ **Null Safety**: Required dependencies are guaranteed to be non-null
- ✅ **Reduced Boilerplate**: Lombok eliminates manual constructor writing

**Trade-offs:**
- ⚠️ **Lombok Dependency**: Requires Lombok annotation processor in build pipeline
- ⚠️ **Large Constructors**: Services with many dependencies can have verbose constructors (e.g., `PrivatePractitionerController` has 7 dependencies)
- ⚠️ **Hidden Implementation**: Actual constructor code is generated, not visible in source

**Implications:**
- Follows Spring best practices and modern dependency injection patterns
- Aligns with the coding conventions document's emphasis on immutability
- Prevents circular dependency issues at compile time
- Compatible with native compilation scenarios (GraalVM)

### 1.2 Component Scanning

**Implementation:**
- Uses `@SpringBootApplication` on main class without explicit `@ComponentScan`
- Relies on default component scanning from package `se.inera.intyg.privatepractitionerservice`
- No custom component scanning filters observed

```java
@SpringBootApplication
public class PrivatePractitionerServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(PrivatePractitionerServiceApplication.class, args);
  }
}
```

**Benefits:**
- ✅ **Convention Over Configuration**: Minimal configuration required
- ✅ **Clear Package Structure**: Scanning scope is well-defined by package hierarchy

**Trade-offs:**
- ⚠️ **Performance**: Scans entire package tree on startup (acceptable for this service size)
- ⚠️ **Implicit Behavior**: No explicit control over what gets scanned

### 1.3 Configuration Classes

**Implementation:**
Configuration is distributed across multiple specialized `@Configuration` classes:

1. **ApplicationConfig** - Core beans (ObjectMapper, MessageSource)
2. **JobConfiguration** - Scheduling and distributed locking
3. **MailServiceConfig** - Email configuration with async support
4. **MailServiceStubConfig** - Test/dev mail stub (profile-specific)
5. **IntygProxyServiceRestClientConfig** - External REST client

**Example:**
```java
@Configuration
public class ApplicationConfig {
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return new CustomObjectMapper();
  }
  
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}
```

**Benefits:**
- ✅ **Separation of Concerns**: Each config class has a single responsibility
- ✅ **Profile Support**: Different configurations for different environments
- ✅ **Explicit Bean Definitions**: Clear visibility of manually configured beans
- ✅ **Type Safety**: Compile-time checking of bean wiring

**Trade-offs:**
- ⚠️ **Fragmentation**: Configuration logic spread across multiple files
- ⚠️ **Mixed Approaches**: Some beans via auto-configuration, some explicit

---

## 2. Configuration Approach

### 2.1 Properties File Strategy

**Implementation:**
- **Primary format**: `.properties` files (not YAML)
- **Main file**: `app/src/main/resources/application.properties`
- **Environment-specific**: `devops/dev/config/application-dev.properties`, `application-integration-test.properties`
- **External configuration support**: Uses `spring.config.additional-location` for environment overrides

**Structure:**
```properties
# Core application
spring.application.name=private-practitioner-service
server.port=8081

# Database
spring.datasource.url=jdbc:mysql://${database.server}:${database.port}/${database.name}...
spring.datasource.username=${database.username}
spring.datasource.password=${database.password}

# Integration endpoints
integration.intygproxyservice.credentialsforperson.endpoint=/api/v1/credentialsForPerson
integration.intygproxyservice.baseurl=http://localhost:18020

# Scheduling
privatlakarportal.hospupdate.cron=0 0 0 1/5 * *
privatlakarportal.hospupdate.interval=14400

# Mail configuration
mail.host=
mail.protocol=smtp
mail.content.approved.subject=Webcert är klar att användas
```

**Benefits:**
- ✅ **IDE Support**: Good autocomplete and validation in IntelliJ
- ✅ **Placeholder Resolution**: `${property}` references work well
- ✅ **Simplicity**: No indentation/nesting complexity
- ✅ **Environment Variable Override**: Standard Spring Boot precedence applies

**Trade-offs:**
- ⚠️ **Flat Structure**: No hierarchical organization (unlike YAML)
- ⚠️ **Repetition**: Property prefixes repeated frequently
- ⚠️ **Less Readable**: Long property names can be verbose
- ⚠️ **Limited Types**: Everything is a string, requires conversion

**Notable Patterns:**
- **Unicode Escapes**: Swedish characters in mail templates use `\u00E4` etc.
- **Sensitive Data Externalization**: Database credentials, API keys use placeholders
- **Feature Flags**: `erase.private.practitioner=true`
- **Structured Logging**: `logging.structured.format.console=ecs`

### 2.2 Profile Management

**Profiles in Use:**
- `dev` - Development environment with bootstrap data
- `testability` - Enables test endpoints
- `mail-stub` - Uses stub email service
- `integration-test` - Integration test configuration
- `h2` - In-memory database for testing

**Profile Activation:**
```java
@Profile({"mail-stub"})
public class MailServiceStubConfig { ... }

@Profile({"dev", TESTABILITY_INIT_DATA_PROFILE})
public class PrivatlakarBootstrapBean { ... }

@Profile(TESTABILITY_PROFILE)
@RestController
public class TestabilityPrivatePractitionerController { ... }
```

**Benefits:**
- ✅ **Environment Isolation**: Different beans for different contexts
- ✅ **Testing Support**: Stubs and test data only in test profiles
- ✅ **Production Safety**: Testability endpoints excluded from production
- ✅ **Flexibility**: Multiple profiles can be active simultaneously

**Trade-offs:**
- ⚠️ **Profile Sprawl**: Need to manage combinations (e.g., "integration-test, testability")
- ⚠️ **Runtime Behavior Variance**: Application behaves differently per profile
- ⚠️ **Testing Complexity**: Must ensure correct profiles active in tests

### 2.3 Configuration Properties Injection

**Implementation:**
Uses `@Value` annotation for property injection in configuration classes:

```java
@Configuration
public class MailServiceConfig {
  @Value("${mail.host}")
  private String mailHost;
  
  @Value("${mail.protocol}")
  private String protocol;
  
  @Value("${mail.port}")
  private String port;
}
```

**Observations:**
- ❌ **No `@ConfigurationProperties`**: Not using type-safe configuration properties
- ❌ **No Validation**: No `@Validated` annotations on configuration classes
- ⚠️ **String Parsing**: Manual conversion (e.g., `Integer.parseInt(port)`)

**Implications:**
- **Missing Benefits**: Type-safe configuration, IDE autocomplete, validation
- **Risk**: Runtime errors if properties malformed or missing
- **Maintenance**: Changes require updating multiple `@Value` annotations

**Recommendation:**
Consider migrating to `@ConfigurationProperties` classes:
```java
@ConfigurationProperties(prefix = "mail")
@Validated
public class MailProperties {
  @NotBlank private String host;
  @NotBlank private String protocol;
  @Positive private int port;
  // ... getters/setters or Lombok
}
```

---

## 3. Security Design

### 3.1 Security Configuration Analysis

**Observation:**
- ❌ **No Spring Security**: No security configuration classes found
- ❌ **No authentication/authorization**: No `@PreAuthorize`, `@Secured`, or security filters
- ❌ **No CSRF protection**: Not configured
- ❌ **No CORS configuration**: Not observed

**Current State:**
The service appears to be an **internal API** (`/internalapi/*` endpoints) intended to run behind:
- API Gateway (handles authentication/authorization)
- Service mesh / reverse proxy
- Internal network boundary

**Endpoints:**
```java
@RestController
@RequestMapping("/internalapi/privatepractitioner")
public class PrivatePractitionerController {
  @PostMapping("") // Register practitioner
  @GetMapping("") // Get practitioner
  @DeleteMapping("/erase/{id}") // Delete practitioner
  @PutMapping("") // Update practitioner
}
```

**Security Mechanisms Present:**

1. **MDC Logging Filter**: Tracks session/trace IDs for audit trail
```java
@Component
public class MdcServletFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    MDC.put(SESSION_ID_KEY, mdcHelper.sessionId(http));
    MDC.put(TRACE_ID_KEY, mdcHelper.traceId(http));
    // ...
  }
}
```

2. **Performance Logging**: Audit trail via AOP
```java
@PerformanceLogging(
  eventAction = "register-private-practitioner", 
  eventType = MdcLogConstants.EVENT_TYPE_CREATION
)
```

3. **Input Validation**: Service-level validation
```java
public class CreateRegistrationRequestValidator {
  public void validate(CreateRegistrationRequest request) {
    // Manual validation logic
  }
}
```

**Benefits of Current Approach:**
- ✅ **Simplicity**: No security overhead for internal service
- ✅ **Centralized Security**: Gateway handles cross-cutting concerns
- ✅ **Performance**: No filter chain processing overhead

**Risks and Considerations:**
- ⚠️ **Trust Boundary**: Assumes network-level security
- ⚠️ **No Defense in Depth**: If exposed accidentally, no protection
- ⚠️ **Audit Trail Only**: Logging present but no preventive controls
- ⚠️ **SSRF Risk**: External integrations (intyg-proxy-service) need validation

**Recommendations:**
For production hardening:
1. Add basic authentication between internal services
2. Implement request signing/verification
3. Add rate limiting (consider Spring Cloud Gateway or resilience4j)
4. Validate all external integration inputs
5. Add security headers via filter or Spring Security

### 3.2 Sensitive Data Handling

**Password/Token Handling:**
- Database credentials externalized via `${database.password}`
- Redis password: `${spring.data.redis.password}`
- No plaintext passwords in version control

**Logging Considerations:**
```java
LOG.warn("Internal exception occured! Internal error code: {} Error message: {}",
    e.getErrorCode(), e.getMessage());
```
- ✅ Does not log full stack traces to clients
- ✅ Uses structured logging (ECS format)
- ⚠️ No evidence of PII masking in logs

### 3.3 Method-Level Security

**Not Implemented:**
- No `@PreAuthorize` annotations
- No role-based access control
- No authentication principal injection

---

## 4. Data Access Choices

### 4.1 JPA and Hibernate

**Implementation:**
- **Spring Data JPA** with Hibernate as provider
- **Database**: MySQL (production), H2 (tests), Testcontainers MySQL (integration tests)
- **Schema Management**: Liquibase migrations

**Configuration:**
```properties
spring.datasource.url=jdbc:mysql://${database.server}:${database.port}/${database.name}?useSSL=false&serverTimezone=Europe/Stockholm
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.liquibase.change-log=classpath:changelog/changelog.xml
```

**Naming Strategy:**
- Uses `PhysicalNamingStrategyStandardImpl` (no camelCase → snake_case conversion)
- Entity fields map directly to uppercase column names

**Benefits:**
- ✅ **Standard JPA**: Portable across JPA providers
- ✅ **Liquibase**: Version-controlled schema migrations
- ✅ **Test Flexibility**: Different databases per environment

**Trade-offs:**
- ⚠️ **Uppercase Columns**: `@Column(name = "PERSONID")` - unconventional style
- ⚠️ **Hibernate Specifics**: Uses `@GenericGenerator` (Hibernate-specific)

### 4.2 Repository Pattern

**Architecture:**
The service uses a **three-layer repository pattern**:

1. **Spring Data JPA Repository** (Data layer)
```java
@Repository
public interface PrivatlakareEntityRepository extends JpaRepository<PrivatlakareEntity, String> {
  @Query("SELECT p from PrivatlakareEntity p WHERE p.hsaId = :hsaId")
  PrivatlakareEntity findByHsaId(@Param("hsaId") String hsaId);
  
  @Query("SELECT p from PrivatlakareEntity p WHERE p.personId = :personId")
  PrivatlakareEntity findByPersonId(@Param("personId") String personId);
}
```

2. **Domain Repository** (Abstraction layer)
```java
@Repository
@RequiredArgsConstructor
public class PrivatePractitionerRepository {
  private final PrivatlakareEntityRepository privatlakareEntityRepository;
  private final PrivatlakareEntityConverter privatlakareEntityConverter;
  
  public Optional<PrivatePractitioner> findByPersonId(String personId) {
    return Optional.ofNullable(privatlakareEntityRepository.findByPersonId(personId))
        .map(privatlakareEntityConverter::convert);
  }
}
```

3. **Service Layer** (Business logic)
```java
@Service
@RequiredArgsConstructor
public class PrivatePractitionerService {
  private final PrivatePractitionerRepository privatePractitionerRepository;
  
  public PrivatePractitionerDTO getPrivatePractitioner(String personOrHsaId) {
    return privatePractitionerRepository.findByPersonId(personOrHsaId)
        .map(updatePrivatePractitionerFromPUService::updateFromPu)
        .map(privatePractitionerConverter::convert)
        .orElseGet(() -> privatePractitionerRepository.findByHsaId(personOrHsaId)...);
  }
}
```

**Benefits:**
- ✅ **Clean Architecture**: Domain layer independent of persistence framework
- ✅ **Domain Model Protection**: Entities not exposed to controllers
- ✅ **Testability**: Can mock domain repository without JPA dependencies
- ✅ **Flexibility**: Can swap persistence implementation without affecting domain

**Trade-offs:**
- ⚠️ **Extra Layer**: More code than direct JPA repository usage
- ⚠️ **Conversion Overhead**: Entity ↔ Domain model conversion on each call
- ⚠️ **Complexity**: Three repository types can confuse new developers

**Entity-Domain Separation:**
```
PrivatlakareEntity (JPA entity) → PrivatePractitioner (domain model) → PrivatePractitionerDTO (API)
```

### 4.3 Query Patterns

**JPQL Queries:**
```java
@Query("SELECT p FROM PrivatlakareEntity p WHERE "
    + "p.privatlakareId NOT IN (SELECT p2.privatlakareId FROM PrivatlakareEntity p2 "
    + "JOIN p2.legitimeradeYrkesgrupper ly WHERE ly.namn = 'Läkare')")
List<PrivatlakareEntity> findWithoutLakarBehorighet();
```

**Patterns Observed:**
- ✅ Explicit `@Query` for complex queries (not derived method names)
- ✅ Named parameters (`:hsaId`)
- ✅ JOIN queries for filtering on collections
- ❌ No native SQL queries observed
- ❌ No Criteria API usage
- ❌ No QueryDSL

**Benefits:**
- ✅ **Type Safety**: JPQL validated at startup
- ✅ **Readability**: JPQL more readable than method name queries
- ✅ **Database Independence**: JPQL portable across databases

**Trade-offs:**
- ⚠️ **Complex Queries**: Some queries quite complex with subqueries
- ⚠️ **N+1 Issues**: Potential with OneToMany relationships

### 4.4 Entity Design

**Example Entity:**
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PRIVATLAKARE")
public class PrivatlakareEntity {
  @Id
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @GeneratedValue(generator = "uuid")
  @Column(name = "PRIVATLAKARE_ID", nullable = false)
  private String privatlakareId;
  
  @Column(name = "PERSONID", nullable = false)
  private String personId;
  
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "PRIVATLAKARE_ID", nullable = false)
  private List<BefattningEntity> befattningar;
  
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "PRIVATLAKARE_ID", nullable = false)
  @Builder.Default
  private List<LegitimeradYrkesgruppEntity> legitimeradeYrkesgrupper = new ArrayList<>();
}
```

**Patterns:**
- ✅ **Lombok**: `@Data`, `@Builder` reduce boilerplate
- ✅ **UUID Primary Keys**: Generated via Hibernate
- ✅ **Cascade Operations**: `CascadeType.ALL` with `orphanRemoval = true`
- ✅ **Bidirectional Relationships**: `@JoinColumn` for foreign keys
- ⚠️ **Mutable Entities**: Using `@Data` (creates setters)
- ⚠️ **Builder with Collections**: `@Builder.Default` needed for lists

**Implications:**
- **Cascade Behavior**: Child entities automatically persisted/deleted
- **Orphan Removal**: Removing from collection deletes from database
- **Lazy Loading**: Collections loaded on access (potential N+1)

### 4.5 Transaction Management

**Strategy:**
- Method-level `@Transactional` annotations
- Explicit transaction boundaries at service layer
- Named transaction manager: `transactionManager`

**Examples:**
```java
@Service
public class CreateRegistrationService {
  @Transactional
  public PrivatePractitionerDTO createRegistration(CreateRegistrationRequest registration) {
    // Multiple repository calls in one transaction
    final var privatePractitioner = privatePractitionerFactory.create(registration);
    final var savedPrivatePractitioner = privatePractitionerRepository.save(privatePractitioner);
    hospRepository.addToCertifier(savedPrivatePractitioner);
    return privatePractitionerConverter.convert(savedPrivatePractitioner);
  }
}

@Transactional(transactionManager = "transactionManager")
public void updateHospInformation() { ... }
```

**Patterns:**
- ✅ **Service-Level Transactions**: Not on repository or controller
- ✅ **Explicit Boundaries**: Clear transaction scope
- ✅ **Read-Write by Default**: No `@Transactional(readOnly = true)` observed

**Benefits:**
- ✅ **ACID Guarantees**: Multi-step operations atomic
- ✅ **Rollback on Exception**: Automatic rollback on RuntimeException
- ✅ **Connection Management**: Automatic connection handling

**Trade-offs:**
- ⚠️ **No Read-Only Optimization**: Could use `readOnly = true` for queries
- ⚠️ **Transaction Scope**: Some transactions might be too broad
- ⚠️ **LazyInitializationException Risk**: If transactions too narrow

**Notable:**
- Scheduled tasks explicitly marked `@Transactional`
- Testability endpoints also transactional

---

## 5. Error Handling

### 5.1 Global Exception Handler

**Implementation:**
```java
@ControllerAdvice
public class PrivatlakarportalRestExceptionHandler {
  
  @ExceptionHandler
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public PrivatlakarportalRestExceptionResponse serviceExceptionHandler(
      HttpServletRequest request, PrivatlakarportalServiceException e) {
    LOG.warn("Internal exception occured! Internal error code: {} Error message: {}",
        e.getErrorCode(), e.getMessage());
    return new PrivatlakarportalRestExceptionResponse(e.getErrorCode(), e.getMessage());
  }
  
  @ExceptionHandler
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public PrivatlakarportalRestExceptionResponse serviceExceptionHandler(
      HttpServletRequest request, RuntimeException re) {
    LOG.error("Unhandled RuntimeException occured!", re);
    return new PrivatlakarportalRestExceptionResponse(
        PrivatlakarportalErrorCodeEnum.UNKNOWN_INTERNAL_PROBLEM, 
        "Unhandled runtime exception");
  }
}
```

**Exception Response Structure:**
```java
public class PrivatlakarportalRestExceptionResponse {
  private PrivatlakarportalErrorCodeEnum errorCode;
  private String message;
}
```

**Benefits:**
- ✅ **Centralized**: All exceptions handled in one place
- ✅ **Consistent Format**: Uniform error response structure
- ✅ **Logging**: Exceptions logged before response
- ✅ **Error Codes**: Enumerated error codes for client handling

**Trade-offs:**
- ⚠️ **All 500 Errors**: No distinction between 400 (client) and 500 (server) errors
- ⚠️ **Generic RuntimeException Handler**: Catches all runtime exceptions as 500
- ⚠️ **No Validation Error Handling**: No `@Valid` usage, no `MethodArgumentNotValidException` handler
- ⚠️ **Stack Trace Exposure Risk**: Full stack trace logged (not sent to client, which is good)

**Missing Handlers:**
- No `@ExceptionHandler` for `HttpMessageNotReadableException` (malformed JSON)
- No handler for `HttpRequestMethodNotSupportedException`
- No custom 404 handling (Spring Boot default)

**Implications:**
- **Security**: Does not leak internal details to clients (positive)
- **Debugging**: Error codes help identify issues
- **Client Experience**: All errors appear as 500, even validation failures

### 5.2 Custom Exceptions

**Example:**
```java
public class PrivatlakarportalServiceException extends RuntimeException {
  private final PrivatlakarportalErrorCodeEnum errorCode;
  
  public PrivatlakarportalServiceException(PrivatlakarportalErrorCodeEnum errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
```

**Benefits:**
- ✅ **Type Safety**: Specific exception types
- ✅ **Error Codes**: Machine-readable error identification
- ✅ **Context**: Can include relevant data in exception

**Usage:**
```java
throw new HospUpdateFailedToContactHsaException("Failed to contact HSA");
```

### 5.3 Validation Strategy

**Manual Validation:**
```java
@Service
public class CreateRegistrationRequestValidator {
  public void validate(CreateRegistrationRequest registration) {
    // Manual validation logic - throws exceptions if invalid
  }
}
```

**Observations:**
- ❌ **No JSR-303/Jakarta Validation**: No `@Valid`, `@NotNull`, `@Size` annotations on DTOs
- ❌ **No Bean Validation**: Manual validation in service layer
- ❌ **No Controller Validation**: No `@Valid` on `@RequestBody` parameters

**Implications:**
- **Manual Work**: Validation logic written by hand
- **Consistency Risk**: Validation may be inconsistent across endpoints
- **Missing Framework Benefits**: No automatic 400 responses, no constraint violation details

**Recommendation:**
Adopt Bean Validation:
```java
public class CreateRegistrationRequest {
  @NotBlank(message = "Person ID is required")
  private String personId;
  
  @Email(message = "Invalid email format")
  private String email;
}

@PostMapping("")
public ResponseEntity<PrivatePractitionerDTO> registerPrivatePractitioner(
    @Valid @RequestBody CreateRegistrationRequest request) { ... }
```

---

## 6. REST API Design

### 6.1 Controller Design

**Pattern:**
```java
@RestController
@RequestMapping("/internalapi/privatepractitioner")
@RequiredArgsConstructor
public class PrivatePractitionerController {
  
  private final CreateRegistrationService createRegistrationService;
  private final UpdatePrivatePractitionerService updatePrivatePractitionerService;
  // ... more services
  
  @PostMapping("")
  @PerformanceLogging(eventAction = "register-private-practitioner", 
                      eventType = MdcLogConstants.EVENT_TYPE_CREATION)
  public ResponseEntity<PrivatePractitionerDTO> registerPrivatePractitioner(
      @RequestBody CreateRegistrationRequest createRegistrationRequest) {
    final var privatePractitionerDTO = createRegistrationService.createRegistration(
        createRegistrationRequest
    );
    return ResponseEntity.ok(privatePractitionerDTO);
  }
  
  @GetMapping("")
  @PerformanceLogging(eventAction = "get-private-practitioner", 
                      eventType = MdcLogConstants.EVENT_TYPE_ACCESSED)
  public ResponseEntity<PrivatePractitionerDTO> getPrivatePractitioner(
      @RequestParam String personOrHsaId) {
    final var privatePractitioner = privatePractitionerService.getPrivatePractitioner(
        personOrHsaId);
    
    if (privatePractitioner == null) {
      return ResponseEntity.notFound().build();
    }
    
    return ResponseEntity.ok(privatePractitioner);
  }
}
```

**Characteristics:**
- ✅ **Thin Controllers**: Delegates to services immediately
- ✅ **ResponseEntity**: Explicit HTTP status codes
- ✅ **RESTful Verbs**: Proper use of `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- ✅ **Performance Logging**: AOP-based logging on each endpoint
- ⚠️ **No URI Variables**: Uses `@RequestParam` instead of `@PathVariable` for GET
- ⚠️ **Empty Path Strings**: `@PostMapping("")` instead of omitting value

**Benefits:**
- ✅ **Separation of Concerns**: Controllers only handle HTTP concerns
- ✅ **Testability**: Business logic in services, not controllers
- ✅ **Observability**: Performance logging on all endpoints

**Trade-offs:**
- ⚠️ **Query Parameters for IDs**: Less RESTful than `/privatepractitioner/{id}`
- ⚠️ **Manual Status Handling**: Could use `@ResponseStatus` or exception mapping

### 6.2 DTO Design

**Request DTO:**
```java
@JsonDeserialize(builder = CreateRegistrationRequestBuilder.class)
@Value
@Builder
public class CreateRegistrationRequest {
  String personId;
  String name;
  String position;
  // ... more fields
  
  @JsonPOJOBuilder(withPrefix = "")
  public static class CreateRegistrationRequestBuilder {
  }
}
```

**Response DTO:**
```java
@JsonDeserialize(builder = PrivatePractitionerDTOBuilder.class)
@Value
@Builder
public class PrivatePractitionerDTO {
  String hsaId;
  String personId;
  String name;
  String email;
  LocalDateTime registrationDate;
  List<CodeDTO> specialties;
  List<CodeDTO> licensedHealthcareProfessions;
  
  @JsonPOJOBuilder(withPrefix = "")
  public static class PrivatePractitionerDTOBuilder {
  }
}
```

**Patterns:**
- ✅ **Immutable DTOs**: Using `@Value` (final fields, no setters)
- ✅ **Builder Pattern**: Enhances readability and flexibility
- ✅ **Jackson Integration**: Custom deserializers for builder pattern
- ✅ **Separate Request/Response**: Clear API contracts
- ✅ **No Annotations**: Fields are pure data (no validation annotations)

**Benefits:**
- ✅ **API Contract Clarity**: Explicit DTOs define API surface
- ✅ **Versioning Flexibility**: Can evolve DTOs independently
- ✅ **Domain Protection**: Internal models not exposed
- ✅ **Immutability**: Aligns with functional programming principles

**Trade-offs:**
- ⚠️ **Boilerplate**: Builder pattern adds code (mitigated by Lombok)
- ⚠️ **No Validation**: DTOs lack constraint annotations
- ⚠️ **Jackson Complexity**: Custom `@JsonPOJOBuilder` configuration required

### 6.3 API Versioning

**Observations:**
- ❌ **No Versioning Strategy**: No `/v1/`, `/v2/` in paths
- ❌ **No Media Type Versioning**: No custom content types
- ⚠️ **Internal API**: `/internalapi/` prefix suggests internal-only

**Implications:**
- **Breaking Changes Risk**: No version allows breaking changes to be deployed
- **Internal Service**: Versioning may be handled at API gateway level
- **Tight Coupling**: Clients and service must evolve together

### 6.4 Content Negotiation

**Default Behavior:**
- JSON only (no XML support observed)
- No custom media types
- Standard `application/json`

**Custom ObjectMapper:**
```java
@Configuration
public class ApplicationConfig {
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return new CustomObjectMapper();
  }
}
```

**Benefits:**
- ✅ **Custom JSON Configuration**: Centralized serialization rules
- ✅ **@Primary**: Ensures this mapper used everywhere

### 6.5 HATEOAS

**Not Implemented:**
- ❌ No Spring HATEOAS usage
- ❌ No hypermedia links in responses
- ❌ No `_links` or `_embedded` structures

**Implications:**
- **Simpler Responses**: Flat JSON structure
- **Client Knowledge**: Clients must know URL structure
- **Not RESTful Level 3**: Richardson Maturity Model level 2

---

## 7. Testing Strategy

### 7.1 Unit Tests

**Pattern:**
```java
@ExtendWith(MockitoExtension.class)
class PrivatePractitionerControllerTest {
  
  @Mock
  private CreateRegistrationService createRegistrationService;
  @Mock
  private UpdatePrivatePractitionerService updatePrivatePractitionerService;
  // ... more mocks
  
  @InjectMocks
  private PrivatePractitionerController privatePractitionerController;
  
  @Test
  void shouldRegisterPrivatePractitioner() {
    when(createRegistrationService.createRegistration(DR_KRANSTEGE_REGISTATION_REQUEST))
        .thenReturn(DR_KRANSTEGE_DTO);
    
    final var actual = privatePractitionerController.registerPrivatePractitioner(
        DR_KRANSTEGE_REGISTATION_REQUEST);
    
    assertAll(
        () -> assertEquals(HttpStatus.OK, actual.getStatusCode()),
        () -> assertEquals(DR_KRANSTEGE_DTO, actual.getBody())
    );
  }
}
```

**Characteristics:**
- ✅ **Mockito**: Standard mocking framework
- ✅ **JUnit 5**: Modern test framework with `@ExtendWith`
- ✅ **Pure Unit Tests**: No Spring context loaded
- ✅ **AssertJ**: Fluent assertions (also uses JUnit assertions)
- ✅ **Test Fixtures**: Shared test data in `testFixtures` source set

**Benefits:**
- ✅ **Fast Execution**: No Spring context overhead
- ✅ **Isolation**: Tests only the controller logic
- ✅ **Clear Dependencies**: Explicit mocks show collaborators
- ✅ **Test Fixtures Module**: Reusable test data across modules

**Trade-offs:**
- ⚠️ **No HTTP Testing**: Not testing actual HTTP layer (serialization, status codes)
- ⚠️ **Manual Mocking**: All dependencies must be mocked

### 7.2 Integration Tests

**Pattern:**
```java
@ActiveProfiles({"integration-test", "testability"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PrivatePractitionerIT {
  
  @LocalServerPort
  private int port;
  
  @Autowired
  private TestRestTemplate restTemplate;
  
  @BeforeAll
  static void beforeAll() {
    Containers.ensureRunning();
  }
  
  @Test
  void shallRegisterPrivatePractitioner() {
    intygProxyServiceMock.credentialsForPersonResponse(...);
    intygProxyServiceMock.certificationPersonResponse(...);
    
    final var response = api.registerPrivatePractitioner(DR_KRANSTEGE_REGISTATION_REQUEST);
    
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
  }
}
```

**Infrastructure:**
- ✅ **Testcontainers**: MySQL, Redis, MockServer containers
- ✅ **Full Spring Context**: `@SpringBootTest` with real beans
- ✅ **Random Port**: Avoids port conflicts
- ✅ **MockServer**: External service mocking
- ✅ **TestRestTemplate**: HTTP client for testing

**Container Management:**
```java
public class Containers {
  public static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.34")
      .withDatabaseName("privatlakarportal");
  
  public static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine")
      .withExposedPorts(6379);
  
  public static final MockServerContainer mockServerContainer = new MockServerContainer(
      DockerImageName.parse("mockserver/mockserver:5.15.0"));
  
  public static void ensureRunning() {
    if (!mySQLContainer.isRunning()) {
      mySQLContainer.start();
      redisContainer.start();
      mockServerContainer.start();
    }
  }
}
```

**Benefits:**
- ✅ **Real Infrastructure**: Tests against actual MySQL, Redis
- ✅ **Isolated**: Each test run uses fresh containers
- ✅ **Repeatability**: Consistent environment
- ✅ **External Service Mocking**: MockServer simulates dependencies
- ✅ **Cleanup**: `@AfterEach` resets state

**Trade-offs:**
- ⚠️ **Slower Execution**: Full Spring context + containers
- ⚠️ **Docker Required**: CI/CD must support Docker
- ⚠️ **Resource Intensive**: Multiple containers per test run

### 7.3 Repository Tests

**Current State:**
```java
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"h2"})
@Transactional
@Disabled("Disabled due to legacy database initialization issues")
class PrivatlakareEntityRepositoryTest {
  @Autowired
  private PrivatlakareEntityRepository privatlakareEntityRepository;
}
```

**Observations:**
- ❌ **Tests Disabled**: Repository tests not running
- ❌ **No @DataJpaTest**: Not using Spring Boot's JPA slice testing
- ⚠️ **H2 Profile**: Separate profile for in-memory database

**Missing Best Practice:**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE) // Use Testcontainers
class PrivatlakareEntityRepositoryTest {
  @Autowired
  private PrivatlakareEntityRepository repository;
  
  @Test
  void shouldFindByPersonId() { ... }
}
```

**Implications:**
- **Limited Coverage**: Repository queries not tested
- **Risk**: Query bugs only found in integration tests
- **Recommendation**: Enable tests with `@DataJpaTest` and Testcontainers

### 7.4 Test Data Management

**Test Fixtures:**
- Dedicated `testFixtures` source set (Gradle `java-test-fixtures` plugin)
- Shared test data classes: `TestDataConstants`, `TestDataDTO`
- Reusable across `app`, `integration-test` modules

**Bootstrap Data:**
```java
@Profile({"dev", TESTABILITY_INIT_DATA_PROFILE})
@PostConstruct
public void initData() {
  final var files = resolver.getResources("classpath:bootstrap-privatlakare/*.json");
  for (Resource res : files) {
    addPrivatlakare(res);
  }
}
```

**Benefits:**
- ✅ **Reusability**: Test data shared across modules
- ✅ **Type Safety**: Strongly typed test data builders
- ✅ **Dev Environment**: Bootstrap data for local development

### 7.5 Test Coverage

**Test Types Present:**
- ✅ Unit tests (controllers, services, converters)
- ✅ Integration tests (full Spring context)
- ✅ Client tests (REST client layer)
- ⚠️ Repository tests (disabled)

**Coverage Tooling:**
```gradle
jacocoTestReport {
  dependsOn test
  
  reports {
    xml.required = true
    html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
  }
  
  afterEvaluate {
    classDirectories.setFrom(files(classDirectories.files.collect {
      fileTree(dir: it, exclude: [
          "**/testability/**",
          "**/integrationtest/**",
      ])
    }))
  }
}
```

**Benefits:**
- ✅ **Jacoco**: Industry-standard coverage tool
- ✅ **XML Reports**: CI/CD integration
- ✅ **Exclusions**: Testability code excluded from coverage

### 7.6 Mocking Strategy

**Frameworks:**
- **Mockito**: Service mocking in unit tests
- **MockServer**: External HTTP service mocking

**Patterns:**
```java
// Unit test mocking
@Mock
private PrivatePractitionerRepository privatePractitionerRepository;

when(privatePractitionerRepository.findByPersonId("123"))
    .thenReturn(Optional.of(practitioner));

// Integration test external service mocking
intygProxyServiceMock.credentialsForPersonResponse(
    fridaKranstegeCredentialsBuilder().build()
);
```

**Benefits:**
- ✅ **Flexible**: Can mock at different levels
- ✅ **Realistic HTTP**: MockServer tests actual HTTP communication
- ✅ **Isolation**: External dependencies don't affect tests

---

## 8. Spring Features Used

### 8.1 Aspect-Oriented Programming (AOP)

**Implementation:**
```java
@Component
@Aspect
@Slf4j
public class PerformanceLoggingAdvice {
  
  @Around("@annotation(performanceLogging)")
  public Object logPerformance(ProceedingJoinPoint joinPoint, PerformanceLogging performanceLogging)
      throws Throwable {
    final var start = LocalDateTime.now();
    var success = true;
    try {
      return joinPoint.proceed();
    } catch (final Throwable throwable) {
      success = false;
      throw throwable;
    } finally {
      final var end = LocalDateTime.now();
      final var duration = Duration.between(start, end).toMillis();
      
      try (final var mdcLogConstants = MdcCloseableMap.builder()
          .put(MdcLogConstants.EVENT_START, start.toString())
          .put(MdcLogConstants.EVENT_DURATION, Long.toString(duration))
          .put(MdcLogConstants.EVENT_ACTION, performanceLogging.eventAction())
          .put(MdcLogConstants.EVENT_OUTCOME, success ? "success" : "failure")
          .build()
      ) {
        log.info("Class: {} Method: {} Duration: {} ms", className, methodName, duration);
      }
    }
  }
}
```

**Custom Annotation:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceLogging {
  String eventType();
  String eventAction();
  String eventCategory() default MdcLogConstants.EVENT_CATEGORY_API;
  boolean isActive() default true;
}
```

**Usage:**
```java
@PostMapping("")
@PerformanceLogging(
  eventAction = "register-private-practitioner", 
  eventType = MdcLogConstants.EVENT_TYPE_CREATION
)
public ResponseEntity<PrivatePractitionerDTO> registerPrivatePractitioner(...) { ... }
```

**Benefits:**
- ✅ **Cross-Cutting Concern**: Logging separated from business logic
- ✅ **Declarative**: Simple annotation-based usage
- ✅ **Structured Logging**: MDC context with timing, outcome, metadata
- ✅ **Non-Invasive**: No boilerplate in controller methods
- ✅ **Configurable**: Can disable via `isActive = false`

**Trade-offs:**
- ⚠️ **Debugging Complexity**: Stack traces include AspectJ proxies
- ⚠️ **Performance Overhead**: Minimal but measurable
- ⚠️ **Implicit Behavior**: Developers must know about aspect

**Implications:**
- **Observability**: Comprehensive performance metrics for all endpoints
- **MDC Integration**: Trace IDs propagate through calls
- **ECS Logging**: Compatible with Elastic Common Schema

### 8.2 Scheduling

**Configuration:**
```java
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT20M")
public class JobConfiguration {
  
  @Bean
  public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
    return new RedisLockProvider(redisConnectionFactory, "pp");
  }
}
```

**Scheduled Task:**
```java
@Scheduled(cron = "${privatlakarportal.hospupdate.cron}")
@SchedulerLock(name = JOB_NAME)
@Transactional
@PerformanceLogging(
  eventAction = "scheduled-update-hosp-information", 
  eventType = MdcLogConstants.EVENT_TYPE_INFO
)
public void scheduledUpdateHospInformation() {
  String skipUpdate = System.getProperty("scheduled.update.skip", "false");
  if ("true".equalsIgnoreCase(skipUpdate)) {
    LOG.info("Skipping scheduled updateHospInformation");
  } else {
    LOG.info("Starting scheduled updateHospInformation");
    updateHospInformation();
  }
}
```

**Features:**
- ✅ **ShedLock**: Distributed locking via Redis (prevents duplicate execution)
- ✅ **Cron Expressions**: Flexible scheduling (`0 0 0 1/5 * *` = every 5 days)
- ✅ **Externalized Schedule**: Cron from properties file
- ✅ **Feature Flag**: Can skip execution via system property
- ✅ **MDC Context**: Trace ID injected for scheduled tasks

**Benefits:**
- ✅ **Distributed Safe**: Multiple instances won't duplicate work
- ✅ **Configurable**: Schedule changeable without code changes
- ✅ **Observability**: Performance logging on scheduled tasks
- ✅ **Graceful Degradation**: Can disable for maintenance

**Trade-offs:**
- ⚠️ **Redis Dependency**: Scheduling requires Redis availability
- ⚠️ **Lock Duration**: Must estimate max execution time (`PT20M`)
- ⚠️ **System Property**: Feature flag via system property (not Spring property)

**Implications:**
- **Scalability**: Safe to run multiple instances
- **Reliability**: Lock prevents concurrent execution
- **Operations**: Can control scheduling without deployment

### 8.3 Async Processing

**Configuration:**
```java
@Configuration
@EnableAsync
public class MailServiceConfig implements AsyncConfigurer {
  
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("async-mail-");
    executor.initialize();
    return executor;
  }
  
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }
}
```

**Usage:**
Async processing likely used for mail sending (configuration present, but usage not visible in analyzed files).

**Benefits:**
- ✅ **Non-Blocking**: Mail sending won't block API responses
- ✅ **Thread Pool**: Controlled resource usage
- ✅ **Exception Handling**: Catches async exceptions

**Trade-offs:**
- ⚠️ **Complexity**: Async errors harder to trace
- ⚠️ **Transaction Context**: Async methods run outside original transaction

### 8.4 Caching

**Not Implemented:**
- ❌ No `@Cacheable`, `@CachePut`, `@CacheEvict` annotations
- ❌ No cache configuration
- ❌ No cache manager bean

**Implications:**
- **Redis Present**: Redis infrastructure available (used for ShedLock)
- **Opportunity**: Could cache code system lookups, HOSP data

### 8.5 Events

**Not Implemented:**
- ❌ No custom `ApplicationEvent` classes
- ❌ No `@EventListener` annotations
- ❌ No `ApplicationEventPublisher` usage

**Alternative:**
Service-to-service calls instead of event-driven architecture:
```java
public PrivatePractitionerDTO createRegistration(...) {
  final var savedPrivatePractitioner = privatePractitionerRepository.save(...);
  hospRepository.addToCertifier(savedPrivatePractitioner); // Direct call
  notifyPrivatePractitionerRegistration.notify(savedPrivatePractitioner); // Direct call
  return convert(savedPrivatePractitioner);
}
```

**Implications:**
- **Tight Coupling**: Services directly call each other
- **Synchronous**: All operations block until complete
- **Simpler**: No event bus complexity

### 8.6 Actuator

**Configuration:**
```gradle
implementation "org.springframework.boot:spring-boot-starter-actuator"
```

**Endpoints:**
Default Spring Boot Actuator endpoints available (health, info, metrics, etc.)

**Benefits:**
- ✅ **Health Checks**: `/actuator/health` for load balancers
- ✅ **Metrics**: Prometheus-compatible metrics
- ✅ **Observability**: Runtime insights

**Not Observed:**
- Custom health indicators
- Custom metrics
- Info endpoint customization

---

## 9. Architecture and Layering

### 9.1 Package Structure

```
se.inera.intyg.privatepractitionerservice/
├── application/                      # Application layer
│   ├── exception/                    # Application-wide exceptions
│   └── privatepractitioner/
│       ├── PrivatePractitionerController.java
│       ├── dto/                      # API DTOs
│       └── service/                  # Application services
│           ├── converter/            # DTO ↔ Domain converters
│           ├── model/                # Domain models
│           ├── validator/            # Business validation
│           └── *Service.java
├── infrastructure/                   # Infrastructure layer
│   ├── codesystem/                   # Code system repository
│   ├── config/                       # Spring configuration
│   ├── logging/                      # Logging infrastructure
│   │   ├── PerformanceLogging.java
│   │   ├── PerformanceLoggingAdvice.java
│   │   └── MdcServletFilter.java
│   ├── mail/                         # Email infrastructure
│   └── persistence/                  # Data persistence
│       ├── entity/                   # JPA entities
│       ├── repository/               # Repositories
│       ├── converter/                # Entity ↔ Domain converters
│       └── bootstrap/                # Data initialization
├── integration/                      # External integrations (separate modules)
│   ├── api/                          # Integration contracts
│   └── intygproxyservice/            # Implementation
└── testability/                      # Test support
```

**Characteristics:**
- ✅ **Clear Separation**: Application, infrastructure, integration layers
- ✅ **Hexagonal Architecture**: Ports (interfaces) and adapters pattern
- ✅ **Domain Models**: Separate from entities and DTOs
- ✅ **Multi-Module**: Integrations in separate Gradle modules

**Benefits:**
- ✅ **Maintainability**: Clear responsibility boundaries
- ✅ **Testability**: Can test domain logic without infrastructure
- ✅ **Flexibility**: Can swap infrastructure implementations
- ✅ **Dependency Direction**: Domain doesn't depend on infrastructure

**Trade-offs:**
- ⚠️ **Complexity**: More layers than simple CRUD app needs
- ⚠️ **Conversion Overhead**: Entity → Domain → DTO transformations
- ⚠️ **Learning Curve**: New developers must understand architecture

### 9.2 Multi-Module Structure

**Modules:**
```
private-practitioner-service/
├── app/                              # Main application
├── integration-api/                  # Integration contracts
├── integration-intyg-proxy-service/  # Proxy service client
└── integration-test/                 # Integration tests
```

**Dependency Graph:**
```
app → integration-api ← integration-intyg-proxy-service
  ↓
integration-test
```

**Benefits:**
- ✅ **Contract-First**: API interfaces separate from implementation
- ✅ **Parallel Development**: Teams can work on different modules
- ✅ **Reusability**: Integration contracts could be shared
- ✅ **Clear Boundaries**: Module boundaries enforce architectural rules

**Build Configuration:**
```gradle
dependencies {
  implementation project(":integration-api")
  runtimeOnly project(":integration-intyg-proxy-service")
}
```

### 9.3 Converter Pattern

**Three Types of Converters:**

1. **Entity ↔ Domain** (Infrastructure layer)
```java
@Component
public class PrivatlakareEntityConverter {
  public PrivatePractitioner convert(PrivatlakareEntity entity) {
    // Convert JPA entity to domain model
  }
}
```

2. **Domain ↔ DTO** (Application layer)
```java
@Component
public class PrivatePractitionerConverter {
  public PrivatePractitionerDTO convert(PrivatePractitioner domain) {
    // Convert domain model to API DTO
  }
}
```

3. **Integration DTOs** (Integration layer)
Handled within integration module

**Benefits:**
- ✅ **Separation**: Each layer has its own model representation
- ✅ **Protection**: Internal models not exposed to external consumers
- ✅ **Evolution**: Can change internal structure without API changes
- ✅ **Type Safety**: Compile-time checking of conversions

**Trade-offs:**
- ⚠️ **Boilerplate**: Conversion code for each model
- ⚠️ **Performance**: Multiple object creations per request
- ⚠️ **Mapping Libraries**: Could use MapStruct to reduce boilerplate

### 9.4 Integration Layer

**Architecture:**
```
App → Integration API (interfaces) → Integration Implementation (REST clients)
```

**Example:**
```java
// Integration API (contract)
public interface HospService {
  HospCredentialsForPerson getHospCredentialsForPersonResponseType(String personalIdentityNumber);
}

// Integration Implementation
@Service
@RequiredArgsConstructor
public class IntygProxyServiceHospService implements HospService {
  private final GetHospCredentialsForPersonService credentialsForPersonService;
  
  @Override
  public HospCredentialsForPerson getHospCredentialsForPersonResponseType(...) {
    return credentialsForPersonService.get(personalIdentityNumber);
  }
}
```

**Benefits:**
- ✅ **Decoupling**: Application code doesn't know about HTTP clients
- ✅ **Testing**: Can mock integration interfaces
- ✅ **Swappable**: Can replace implementation (e.g., gRPC instead of REST)

**REST Client:**
```java
@Configuration
public class IntygProxyServiceRestClientConfig {
  @Bean(name = "intygProxyServiceRestClient")
  public RestClient ipsRestClient() {
    return RestClient.create(intygProxyServiceBaseUrl);
  }
}
```

**Note:**
- Uses Spring 6's new `RestClient` (not legacy `RestTemplate`)
- Modern, fluent API for HTTP calls

---

## 10. Key Findings and Recommendations

### 10.1 Strengths

1. **Clean Architecture**: Excellent separation of concerns with clear layers
2. **Constructor Injection**: Consistent use of modern DI patterns
3. **Observability**: Comprehensive performance logging via AOP
4. **Testability**: Well-structured with unit and integration tests
5. **Distributed Scheduling**: ShedLock prevents duplicate job execution
6. **Multi-Module**: Clear boundaries between integration contracts and implementations
7. **Immutability**: DTOs and domain models use immutable patterns
8. **Modern Spring**: Uses Spring Boot 3.x, RestClient, Java 21 features

### 10.2 Areas for Improvement

#### High Priority

1. **Security**
   - **Issue**: No authentication/authorization mechanisms
   - **Risk**: If accidentally exposed, no protection
   - **Recommendation**: Add basic auth between services, implement request signing

2. **Input Validation**
   - **Issue**: No Bean Validation (`@Valid`, JSR-303)
   - **Risk**: Invalid data can reach service layer
   - **Recommendation**: Add validation annotations on DTOs
   ```java
   @NotBlank(message = "Person ID required")
   private String personId;
   ```

3. **Configuration Properties**
   - **Issue**: Using `@Value` instead of `@ConfigurationProperties`
   - **Risk**: No type safety, validation, or IDE support
   - **Recommendation**: Migrate to `@ConfigurationProperties` classes
   ```java
   @ConfigurationProperties(prefix = "mail")
   @Validated
   public class MailProperties { ... }
   ```

4. **Repository Tests**
   - **Issue**: JPA repository tests disabled
   - **Risk**: Query bugs not caught until integration tests
   - **Recommendation**: Enable with `@DataJpaTest` and Testcontainers

#### Medium Priority

5. **Error Handling**
   - **Issue**: All errors return 500 status
   - **Improvement**: Distinguish 400 (client error) from 500 (server error)
   - **Recommendation**: Add validation error handlers, use appropriate status codes

6. **API Design**
   - **Issue**: No versioning strategy, query params instead of path variables
   - **Improvement**: Add `/v1/` prefix, use `/privatepractitioner/{id}` pattern
   - **Recommendation**: Plan API evolution strategy

7. **Caching**
   - **Issue**: No caching despite Redis available
   - **Opportunity**: Cache code systems, HOSP lookups
   - **Recommendation**: Add `@Cacheable` on read-heavy operations

8. **Transaction Optimization**
   - **Issue**: No read-only transactions
   - **Improvement**: Use `@Transactional(readOnly = true)` for queries
   - **Benefit**: Database optimization hints

#### Low Priority

9. **Event-Driven Architecture**
   - **Current**: Synchronous service calls
   - **Consideration**: Could use Spring Events for notification
   - **Benefit**: Looser coupling, async processing

10. **Metrics**
    - **Current**: Default Actuator metrics only
    - **Improvement**: Custom business metrics (registrations per hour, etc.)
    - **Recommendation**: Add Micrometer custom metrics

### 10.3 Best Practices Followed

- ✅ Constructor injection with `@RequiredArgsConstructor`
- ✅ Immutable DTOs with builder pattern
- ✅ Separation of entities, domain models, and DTOs
- ✅ Performance logging via AOP
- ✅ Structured logging with MDC
- ✅ Testcontainers for integration tests
- ✅ Multi-module project structure
- ✅ Profile-based configuration
- ✅ Liquibase for database migrations
- ✅ Distributed locking for scheduled tasks

### 10.4 Technology Stack Summary

| Category | Technology | Version/Notes |
|----------|-----------|---------------|
| **Core** | Spring Boot | 3.x |
| **Java** | OpenJDK | 21 |
| **Web** | Spring MVC | `spring-boot-starter-web` |
| **Data** | Spring Data JPA | Hibernate provider |
| **Database** | MySQL | Production |
| | H2 | Tests (currently disabled) |
| | Testcontainers | Integration tests |
| **Migrations** | Liquibase | Schema versioning |
| **Scheduling** | Spring Scheduling | `@Scheduled` |
| **Locking** | ShedLock | Redis-based distributed locks |
| **Caching** | Redis | Infrastructure (not used for caching) |
| **HTTP Client** | RestClient | Spring 6 modern API |
| **AOP** | AspectJ | Performance logging |
| **Validation** | Manual | No Bean Validation |
| **Testing** | JUnit 5 | Jupiter |
| | Mockito | Mocking framework |
| | Testcontainers | MySQL, Redis, MockServer |
| | AssertJ | Fluent assertions |
| **Build** | Gradle | 8.x with Kotlin DSL for build logic |
| **Observability** | Actuator | Health, metrics |
| | SLF4j/Logback | Structured logging (ECS) |
| **Lombok** | Lombok | Boilerplate reduction |

---

## 14. Conclusion

The Private Practitioner Service demonstrates a **well-architected Spring Boot application** with strong separation of concerns, modern patterns, and good observability. The codebase follows many Spring best practices, particularly in dependency injection, transaction management, and testing infrastructure.

The most significant gaps are in **security** (acceptable for internal service) and **input validation** (should be addressed). The decision to use **properties files over YAML** and **manual validation over Bean Validation** are architectural choices that work but miss some framework benefits.

The **multi-module structure** with integration abstractions shows maturity, while the **performance logging AOP** and **distributed scheduling with ShedLock** demonstrate advanced Spring usage. The **three-layer repository pattern** (JPA → Domain → Service) provides excellent separation but adds complexity appropriate for a domain-rich application.

Overall, this is a **solid, maintainable Spring Boot service** with clear architecture and good operational characteristics. The recommendations focus on incremental improvements rather than fundamental changes.

---

**Document Version:** 1.0  
**Analysis Date:** November 26, 2025  
**Analyzed Codebase:** private-practitioner-service (Spring Boot 3.x, Java 21)

