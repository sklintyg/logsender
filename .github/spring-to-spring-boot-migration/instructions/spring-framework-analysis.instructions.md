# Spring Framework to Spring Boot Migration - Analysis Instructions

This document provides step-by-step instructions for analyzing a Spring Framework application with the goal of migrating it to Spring Boot. The analysis should be thorough, identifying all components that need migration while enabling an incremental, non-breaking migration approach.

---

## Analysis Principles

1. **Incremental Migration**: Each analysis section should enable migration steps that leave the application functional
2. **Mark Uncertainties**: Use `OBSERVE` for questions requiring developer input
3. **Comprehensive Coverage**: Analyze all aspects of the Spring Framework application
4. **Prioritize Dependencies**: Identify foundational changes before dependent features
5. **Document Everything**: Record current state and migration path clearly

---

## Analysis Workflow

### Phase 1: Project Structure and Build Analysis

#### 1.1 Build Configuration Analysis

**Objective**: Understand the current build setup and identify changes needed for Spring Boot.

**Tasks**:
1. **Locate build file** (`build.gradle`, `build.gradle.kts`, `pom.xml`)
2. **Document current setup**:
   - Build tool (Gradle/Maven) and version
   - Java version (source/target compatibility)
   - Packaging type (WAR/JAR)
   - Plugin configuration
3. **Analyze dependencies**:
   - List all Spring Framework dependencies
   - Identify Spring versions in use
   - Find application server dependencies (Tomcat, Jetty, etc.)
   - Identify common/infra library dependencies to be removed
   - List third-party libraries (Apache Camel, logging, etc.)
4. **Check for dependency management**:
   - BOMs (Bill of Materials) currently used
   - Version properties
   - Dependency exclusions

**Output**:
- Current build configuration summary
- List of dependencies to add (Spring Boot starters)
- List of dependencies to remove (Spring Framework, server dependencies)
- List of dependencies to update (versions)
- `OBSERVE` if custom plugins need migration consideration

**Migration Notes**:
- Plan to add Spring Boot Gradle/Maven plugin
- Plan packaging change from WAR to JAR
- Identify need for intyg-bom, Spring Boot BOM, Camel BOM

---

#### 1.2 Project Structure Analysis

**Objective**: Map the current project structure and identify organizational changes.

**Tasks**:
1. **Document source structure**:
   - Main source directory (`src/main/java`)
   - Resource directory (`src/main/resources`)
   - Webapp directory (`src/main/webapp`)
   - Test directories
2. **Identify base package**: Root package for component scanning
3. **List configuration locations**:
   - XML configuration files location
   - Properties files location
   - Logback/Log4j configuration
4. **Check for multi-module structure**: Sub-projects or modules
5. **Review package organization**: Layer separation (controller, service, repository, etc.)

**Output**:
- Package structure diagram
- Base package for `@SpringBootApplication`
- Files to migrate/relocate
- `OBSERVE` if non-standard structure requires special handling

**Migration Notes**:
- Plan for `src/main/webapp` content migration (if web app)
- Identify location for new `@SpringBootApplication` class
- Plan for resource file reorganization if needed

---

### Phase 2: Spring Configuration Analysis

#### 2.1 XML Configuration Analysis

**Objective**: Identify all XML-based Spring configuration to convert to Java config.

**Tasks**:
1. **Locate all Spring XML files**:
   - `applicationContext.xml`
   - `web.xml` (servlet configuration)
   - Any context files in `WEB-INF`
   - Spring configuration in resources
2. **For each XML file, document**:
   - Bean definitions (class, id, scope, init-method, destroy-method)
   - Property references and values
   - Component scan configurations
   - Import statements to other XML files
   - Profile-specific configurations
   - Property placeholder configurations
3. **Identify bean types**:
   - Infrastructure beans (DataSource, EntityManagerFactory, TransactionManager)
   - Application beans (Services, Repositories, Controllers)
   - Third-party integrations (Camel, HTTP clients, etc.)
   - Filter and listener definitions
4. **Document property sources**:
   - PropertyPlaceholderConfigurer
   - Property file locations
   - Environment-specific properties

**Output**:
- Complete inventory of XML configuration files
- Bean-by-bean migration plan
- Property migration plan
- `OBSERVE` for complex bean configurations requiring design decisions

**Migration Notes**:
- Plan `@Configuration` classes to replace XML
- Identify beans that can use autoconfiguration
- Plan property migration to `application.properties`

---

#### 2.2 Java Configuration Analysis

**Objective**: Identify existing Java-based configuration and how it integrates with Spring Boot.

**Tasks**:
1. **Locate `@Configuration` classes**
2. **Document each configuration class**:
   - Purpose and scope
   - Bean definitions
   - Conditional logic
   - Profile usage
   - Property injection
3. **Check compatibility**:
   - Are configurations Spring Boot compatible?
   - Any WildFly/server-specific configurations?
   - Any conflicting autoconfiguration?

**Output**:
- List of existing `@Configuration` classes
- Compatibility assessment
- Required modifications
- `OBSERVE` for configuration conflicts

**Migration Notes**:
- Identify which configurations can remain unchanged
- Plan integration with Spring Boot autoconfiguration
- Identify configuration that should use Spring Boot starters

---

#### 2.3 Component Scanning Analysis

**Objective**: Understand current component scanning setup.

**Tasks**:
1. **Find component scan configurations**:
   - XML `<context:component-scan>` elements
   - `@ComponentScan` annotations
2. **Document**:
   - Base packages scanned
   - Include/exclude filters
   - Scope and use-default-filters settings
3. **Identify all stereotyped components**:
   - `@Component`, `@Service`, `@Repository`, `@Controller`
   - Custom stereotype annotations

**Output**:
- Component scanning configuration summary
- List of all components to be scanned
- `OBSERVE` if custom scanning logic needs review

**Migration Notes**:
- Plan to use `@SpringBootApplication` default scanning
- Identify if custom filters needed

---

### Phase 3: Apache Camel Analysis (if applicable)

#### 3.1 Camel Configuration Analysis

**Objective**: Understand Apache Camel setup for migration to Camel Spring Boot.

**Tasks**:
1. **Locate Camel configuration**:
   - `camel-context.xml` files
   - Java DSL RouteBuilder classes
   - Camel property configurations
2. **Document Camel version**: Current version in use
3. **Identify Camel components used**:
   - HTTP/HTTPS endpoints
   - JMS/messaging
   - File processing
   - Timers/schedulers
   - Data transformation
4. **Analyze Camel beans**:
   - Processors
   - Converters
   - Aggregators
   - Custom components

**Output**:
- Camel component inventory
- Camel version and compatibility notes
- List of Camel Spring Boot starters needed
- `OBSERVE` for complex Camel configurations

**Migration Notes**:
- Plan conversion of XML routes to Java DSL
- Identify Camel Spring Boot starter dependencies
- Plan Camel property migration

---

#### 3.2 Camel Routes Analysis

**Objective**: Document all Camel routes for conversion to Java DSL.

**Tasks**:
1. **For each route, document**:
   - Route ID
   - From endpoint
   - Processing steps (processors, transformations)
   - To endpoint(s)
   - Error handling (onException, errorHandler)
   - Route policies
2. **Identify route dependencies**:
   - Spring beans referenced
   - External endpoints
   - Data formats
3. **Document route patterns**:
   - Direct routes
   - Content-based routing
   - Aggregation patterns
   - Splitter patterns
   - Error handling patterns

**Output**:
- Complete route inventory
- Route dependency map
- Java DSL conversion plan for each route
- `OBSERVE` for complex routing logic

**Migration Notes**:
- Create RouteBuilder classes for each logical group
- Plan for Spring-managed RouteBuilder components
- Maintain route logic exactly as-is

---

### Phase 4: Dependency Injection Analysis

#### 4.1 Bean Dependency Analysis

**Objective**: Understand how beans are wired and plan for constructor injection.

**Tasks**:
1. **For each bean, identify**:
   - Dependency injection method (setter, constructor, field)
   - Dependencies (other beans, properties)
   - Lifecycle callbacks (@PostConstruct, @PreDestroy, init-method)
   - Scope (singleton, prototype, request, session)
2. **Document injection patterns**:
   - `@Autowired` on fields
   - `@Autowired` on setters
   - `@Autowired` on constructors
   - `@Resource` annotations
   - `@Inject` annotations
3. **Identify circular dependencies**: Beans that depend on each other

**Output**:
- Dependency graph
- Beans requiring refactoring to constructor injection
- Circular dependency issues to resolve
- `OBSERVE` for complex dependency scenarios

**Migration Notes**:
- Plan conversion to constructor injection with `@RequiredArgsConstructor`
- Add `final` to all injected fields
- Resolve circular dependencies

---

### Phase 5: Web Layer Analysis (if applicable)

#### 5.1 Servlet Configuration Analysis

**Objective**: Identify servlet, filter, and listener configurations for migration.

**Tasks**:
1. **Analyze `web.xml`**:
   - Servlet definitions
   - Filter definitions and mappings
   - Listener definitions
   - Context parameters
   - Error pages
   - Session configuration
2. **Identify Spring MVC configuration**:
   - DispatcherServlet configuration
   - View resolvers
   - Message converters
   - Interceptors
3. **Document filters**:
   - Character encoding filters
   - CORS filters
   - Security filters
   - MDC/logging filters

**Output**:
- Complete servlet/filter/listener inventory
- Migration plan for each component
- `OBSERVE` for complex filter chains

**Migration Notes**:
- Plan to remove `web.xml` (use annotations/Spring Boot)
- Convert filters to `@Component` implementing `Filter`
- Leverage Spring Boot autoconfiguration for common patterns

---

#### 5.2 REST Controller Analysis (if applicable)

**Objective**: Document REST endpoints and ensure Spring Boot compatibility.

**Tasks**:
1. **Locate all controllers**:
   - `@Controller` classes
   - `@RestController` classes
2. **For each controller, document**:
   - Base request mapping
   - Endpoint methods
   - HTTP methods (GET, POST, PUT, DELETE)
   - Request/response DTOs
   - Exception handling
3. **Identify global components**:
   - `@ControllerAdvice` classes
   - Exception handlers
   - Response body advice

**Output**:
- REST API inventory
- Compatibility assessment
- Required changes
- `OBSERVE` for non-standard patterns

**Migration Notes**:
- Verify Spring MVC annotations are Spring Boot compatible
- Plan for any endpoint changes

---

### Phase 6: Data Access Analysis (if applicable)

#### 6.1 Data Source Configuration Analysis

**Objective**: Identify database configuration for Spring Boot migration.

**Tasks**:
1. **Locate data source configuration**:
   - DataSource bean definitions
   - Connection pool configuration (HikariCP, DBCP, etc.)
   - Database URL, username, password sources
2. **Document database technology**:
   - Database type (MySQL, PostgreSQL, Oracle, etc.)
   - JDBC driver
3. **Identify transaction management**:
   - TransactionManager bean
   - `@Transactional` usage
   - Transaction propagation patterns

**Output**:
- Data source configuration summary
- Spring Boot property migration plan
- `OBSERVE` for custom data source configurations

**Migration Notes**:
- Plan to use Spring Boot datasource autoconfiguration
- Migrate to `application.properties` (spring.datasource.*)
- Use HikariCP (Spring Boot default)

---

#### 6.2 JPA/Hibernate Configuration Analysis (if applicable)

**Objective**: Document JPA/Hibernate setup for Spring Boot.

**Tasks**:
1. **Locate JPA configuration**:
   - EntityManagerFactory bean
   - JPA vendor adapter
   - Entity packages
   - Hibernate properties
2. **Document**:
   - Hibernate dialect
   - DDL auto setting
   - Naming strategies
   - Second-level cache configuration
3. **Identify repositories**:
   - Spring Data JPA repositories
   - Custom repository implementations
   - Query methods

**Output**:
- JPA configuration summary
- Spring Boot property migration plan
- Repository inventory
- `OBSERVE` for complex JPA setups

**Migration Notes**:
- Use Spring Boot JPA autoconfiguration
- Migrate to `spring.jpa.*` properties
- Add `spring-boot-starter-data-jpa` if needed

---

### Phase 7: Logging Configuration Analysis

#### 7.1 Current Logging Analysis

**Objective**: Understand current logging setup for migration to ECS structured logging.

**Tasks**:
1. **Identify logging framework**:
   - Logback, Log4j, Log4j2, JUL
   - Configuration file location
2. **Analyze logging configuration**:
   - Appenders (console, file, syslog)
   - Log levels per package
   - Log patterns
   - Rolling policies
3. **Document MDC usage**:
   - MDC keys set in code
   - MDC propagation mechanisms
4. **Check for logging filters/interceptors**

**Output**:
- Current logging configuration summary
- ECS migration plan
- MDC key inventory
- `OBSERVE` for custom logging requirements

**Migration Notes**:
- Plan migration to `logback-spring.xml`
- Add `logstash-logback-encoder` for ECS
- Configure structured logging properties
- Implement MDC filter as Spring component

---

### Phase 8: Properties and Configuration Analysis

#### 8.1 Property Files Analysis

**Objective**: Inventory all property files for migration to Spring Boot properties.

**Tasks**:
1. **Locate all property files**:
   - `.properties` files
   - `.yml` or `.yaml` files
   - XML property files
   - Environment-specific files
2. **For each property file, document**:
   - Purpose and scope
   - Property keys and values
   - Placeholder usage
   - Profile associations
3. **Identify property sources**:
   - System properties
   - Environment variables
   - External configuration locations
   - Encrypted properties

**Output**:
- Complete property inventory
- Migration mapping to Spring Boot conventions
- Environment-specific configuration plan
- `OBSERVE` for sensitive properties handling

**Migration Notes**:
- Consolidate to `application.properties`
- Create profile-specific files (`application-{profile}.properties`)
- Use Spring Boot property naming conventions
- Plan for externalized configuration

---

### Phase 9: Testing Infrastructure Analysis

#### 9.1 Test Configuration Analysis

**Objective**: Assess current test setup and plan Spring Boot test migration.

**Tasks**:
1. **Identify test frameworks**:
   - JUnit version (4 or 5)
   - Test runner (SpringJUnit4ClassRunner, SpringRunner, SpringExtension)
   - Mocking framework (Mockito, EasyMock)
2. **Analyze test types**:
   - Unit tests (no Spring context)
   - Integration tests (with Spring context)
   - End-to-end tests
3. **Document test configurations**:
   - Test context configuration
   - Test property sources
   - Test profiles
   - Mock beans
4. **Identify test data management**:
   - Test fixtures
   - Database initialization
   - Mock services

**Output**:
- Test infrastructure summary
- Spring Boot test migration plan
- Test dependencies to add/update
- `OBSERVE` for complex test scenarios

**Migration Notes**:
- Migrate to JUnit 5 if needed
- Use `@SpringBootTest` for integration tests
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Plan for Testcontainers if needed

---

### Phase 10: Deployment and Packaging Analysis

#### 10.1 Deployment Configuration Analysis

**Objective**: Understand current deployment model and plan for Spring Boot JAR.

**Tasks**:
1. **Document current deployment**:
   - Packaging type (WAR/JAR)
   - Application server (Tomcat, Jetty, WildFly)
   - Server version
   - Deployment descriptor files
2. **Identify server-specific configurations**:
   - JNDI resources
   - Server-provided libraries
   - Shared libraries
3. **Document Docker/containerization**:
   - Dockerfile
   - Base images
   - Entry points
4. **Review startup scripts and configurations**

**Output**:
- Current deployment model summary
- Spring Boot JAR migration plan
- Docker update plan
- `OBSERVE` for complex deployment scenarios

**Migration Notes**:
- Plan change from WAR to executable JAR
- Update Dockerfile for Spring Boot JAR
- Remove application server dependencies
- Plan for embedded Tomcat configuration

---

### Phase 11: External Integration Analysis

#### 11.1 External Service Integration Analysis

**Objective**: Document external service integrations for Spring Boot compatibility.

**Tasks**:
1. **Identify external integrations**:
   - REST API calls
   - SOAP web services
   - Message queues (JMS, AMQP, Kafka)
   - Email services
   - File systems
2. **Document integration mechanisms**:
   - HTTP clients (RestTemplate, HttpClient, etc.)
   - JMS templates
   - Web service clients
3. **Analyze configuration**:
   - Endpoint URLs (hardcoded vs. externalized)
   - Timeouts and retry logic
   - Authentication/credentials
   - Connection pooling

**Output**:
- External integration inventory
- Spring Boot migration plan (e.g., use RestClient)
- Configuration externalization plan
- `OBSERVE` for complex integration patterns

**Migration Notes**:
- Consider migrating to Spring 6's `RestClient`
- Externalize all endpoint configurations
- Use Spring Boot starters for common integrations

---

### Phase 12: Security Analysis (if applicable)

#### 12.1 Security Configuration Analysis

**Objective**: Document security setup and plan migration (or removal if not needed).

**Tasks**:
1. **Identify security framework**:
   - Spring Security
   - Custom security filters
   - No security (delegated to gateway/proxy)
2. **If Spring Security, document**:
   - Security configuration classes
   - Authentication mechanisms
   - Authorization rules
   - CSRF protection
   - CORS configuration
3. **Document authentication**:
   - User details service
   - Password encoders
   - Authentication providers
4. **Document authorization**:
   - Method-level security
   - URL-based security
   - Role hierarchy

**Output**:
- Security configuration summary
- Spring Boot Security migration plan (if applicable)
- `OBSERVE` if security model needs redesign

**Migration Notes**:
- If using Spring Security, ensure Spring Boot compatibility
- If no security, document trust boundary assumptions
- Consider adding basic security for defense in depth

---

## Analysis Output Format

For each phase, create a section in the application-specific migration guide with:

1. **Current State**: What exists today
2. **Target State**: What should exist after migration
3. **Migration Steps**: Ordered steps to get from current to target
4. **Dependencies**: What must be done first
5. **Testing Strategy**: How to verify this migration step
6. **Rollback Plan**: How to undo if needed
7. **OBSERVE Flags**: Developer decisions needed

---

## Incremental Migration Strategy

Group analysis findings into these migration phases:

### Phase A: Foundation (do first)
- Build configuration updates
- Dependency management
- Create main application class
- Basic Spring Boot configuration

### Phase B: Core Configuration (do second)
- XML to Java config migration
- Property file consolidation
- Component scanning setup

### Phase C: Apache Camel Migration (if applicable)
- Camel Spring Boot integration
- Route conversion to Java DSL
- Camel configuration migration

### Phase D: Web and API Layer
- Servlet/filter migration
- Controller updates
- REST endpoint verification

### Phase E: Data Access (if applicable)
- DataSource configuration
- JPA/Hibernate setup
- Repository verification

### Phase F: Cross-Cutting Concerns
- Logging migration to ECS
- MDC filter implementation
- Performance logging (AOP)
- Actuator setup

### Phase G: Testing
- Test framework updates
- Integration test migration
- Test coverage verification

### Phase H: Packaging and Deployment
- WAR to JAR conversion
- Docker updates
- Deployment verification

---

## Important Reminders

1. **Always mark uncertainties with `OBSERVE`** - Don't guess on critical decisions
2. **Document every bean and configuration** - Nothing should be missed
3. **Check for hidden dependencies** - XML imports, classpath scanning, etc.
4. **Verify Spring Boot autoconfiguration compatibility** - Some custom configs may conflict
5. **Consider the big picture** - How components interact, not just individual pieces
6. **Plan for testing at each step** - Each increment must be verifiable
7. **Document WHY, not just WHAT** - Explain reasoning for migration decisions

---

**This analysis should result in a comprehensive, application-specific migration guide that enables safe, incremental migration from Spring Framework to Spring Boot.**

