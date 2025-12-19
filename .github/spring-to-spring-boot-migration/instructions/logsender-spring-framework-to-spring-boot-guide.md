# Logsender: Spring Framework to Spring Boot 3.5.8 Migration Guide

**Application:** logsender  
**Current State:** Spring Framework with XML/Java configuration, Apache Camel 4.8.0, WAR packaging  
**Target State:** Spring Boot 3.5.8, Java 21, Apache Camel Spring Boot, JAR packaging
**Created:** 2025-12-08

---

## Executive Summary

This guide provides a comprehensive, step-by-step plan to migrate the logsender application from Spring Framework to Spring Boot 3.5.8. The application currently uses:
- Spring Framework with XML configuration (`camel-context.xml`, `basic-cache-config.xml`) and Java configuration
- Apache Camel 4.8.0 for message routing
- WAR packaging deployed on external Tomcat
- Gretty for local development
- ActiveMQ for JMS messaging
- Redis for caching (profile-based)
- External common and infra library dependencies (to be removed)
- Separate modules for PDL models and stub services (to be integrated)
- Manual bean configuration

The migration will transform it to:
- **Spring Boot 3.5.8 with 100% Java configuration** (zero XML)
- Apache Camel Spring Boot integration with Java DSL routes
- JAR packaging with embedded Tomcat
- Spring Boot autoconfiguration wherever possible
- Spring Data Redis for caching
- Structured ECS logging with Logback
- Constructor injection with Lombok
- All module code integrated directly into main application structure

---

## Migration Strategy

**Key Migration Goals:**
1. **Zero XML Configuration** - All XML config files (`camel-context.xml`, `basic-cache-config.xml`) will be converted to Java `@Configuration` classes
2. **Integrated Codebase** - Separate module code will be integrated directly into the main source tree
3. **Spring Boot Native** - Leverage Spring Boot autoconfiguration and conventions
4. **Modern Java Patterns** - Constructor injection, immutability, declarative configuration

The migration is divided into **8 incremental phases** (A-H) that keep the application functional after each step:

1. **Phase A: Foundation** - Build and dependency updates
2. **Phase B: Core Configuration** - Remove XML, create Spring Boot main class
3. **Phase C: Apache Camel Migration** - Camel Spring Boot integration
4. **Phase D: Remove Common/Infra Dependencies** - Replace with Spring Boot equivalents
5. **Phase E: Logging Migration** - ECS structured logging
6. **Phase F: Configuration Cleanup** - Properties consolidation
7. **Phase G: Testing Updates** - Spring Boot test framework
8. **Phase H: Packaging & Deployment** - WAR to JAR conversion

---

## Phase A: Foundation - Build Configuration Updates

### Current State

**Build Tool:** Gradle with:
- `war` plugin for WAR packaging
- `org.gretty` plugin for local Tomcat development
- `io.spring.dependency-management` version 1.1.6
- Manual Spring Framework dependencies
- Common/infra dependencies: `commonVersion = 4.0.0-SNAPSHOT`, `infraVersion = 4.0.0-SNAPSHOT`
- Apache Camel 4.8.0
- Spring Boot BOM `springbootMavenBomVersion = "3.3.4"`
- Java 21 source/target compatibility

**Key Dependencies to Remove:**
```groovy
implementation "se.inera.intyg.common:integration-util:${commonVersion}"
implementation "se.inera.intyg.common:logging-util:${commonVersion}"
implementation "se.inera.intyg.infra:monitoring:${infraVersion}"
```

**Modules to Integrate:**
The `loggtjanst/log-messages` and `loggtjanst/loggtjanst-stub` folders contain code that will be **integrated directly into the main logsender project** during migration. They are currently separate for context but will be moved into the appropriate packages within `src/main/java/se/inera/intyg/logsender/`.

**Current Camel Dependencies:**
```groovy
implementation "org.apache.camel:camel-activemq:${camelVersion}"
implementation "org.apache.camel:camel-core:${camelVersion}"
runtimeOnly "org.apache.camel:camel-jms:${camelVersion}"
runtimeOnly "org.apache.camel:camel-spring:${camelVersion}"
runtimeOnly "org.apache.camel:camel-spring-xml:${camelVersion}"
```

### Target State

**Build Configuration:**
- Remove `war` and `org.gretty` plugins
- Add `org.springframework.boot` plugin version 3.5.8
- Update `io.spring.dependency-management` to 1.1.7
- Add intyg-bom for dependency management
- Replace standalone Camel with Camel Spring Boot starters
- Remove common/infra dependencies
- Change packaging to JAR

### Migration Steps

#### Step A.1: Update build.gradle plugins

**Action:**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.8'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'idea'
    id 'org.sonarqube' version '5.1.0.4882'
    id 'org.owasp.dependencycheck' version '10.0.4'
    id 'jacoco'
    // REMOVE: id 'war'
    // REMOVE: id "org.gretty" version "4.1.5"
}
```

**Testing:** Run `./gradlew clean build` - should compile (may have errors to fix later)

#### Step A.2: Update dependency management

**Action:**
```groovy
ext {
    // REMOVE: commonVersion
    // REMOVE: infraVersion
    
    rivtaStoreLogSchemasVersion = "2.0.1.2"
    schemasContractVersion = "2.1.8.2"

    apacheCXFVersion = "4.0.5"
    camelSpringBootVersion = "4.8.0" // Camel Spring Boot version
    geronimoVersion = "1.1.1"
    googleGuavaVersion = "33.0.0-jre"
    jakartaJwsApiVersion = "3.0.0"
    jaxb2Version = "3.0.0"
    logbackEcsEncoderVersion = "1.6.0"
    
    intygBomVersion = "0.1.0.5" 
}

dependencyManagement {
    imports {
        mavenBom "se.inera.intyg:intyg-bom:${intygBomVersion}"
        mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
        mavenBom "org.apache.camel.springboot:camel-spring-boot-bom:${camelSpringBootVersion}"
    }
}
```

#### Step A.3: Add Spring Boot dependencies

**Action:**
```groovy
dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Camel Spring Boot
    implementation "org.apache.camel.springboot:camel-spring-boot-starter:${camelSpringBootVersion}"
    implementation "org.apache.camel.springboot:camel-activemq-starter:${camelSpringBootVersion}"
    implementation "org.apache.camel.springboot:camel-jms-starter:${camelSpringBootVersion}"
    
    // Existing dependencies (keep)
    implementation "co.elastic.logging:logback-ecs-encoder:${logbackEcsEncoderVersion}"
    implementation "se.inera.intyg.schemas:schemas-contract:${schemasContractVersion}"
    implementation "se.riv.informationsecurity.auditing.log:informationsecurity-auditing-log-schemas:${rivtaStoreLogSchemasVersion}"
    
    // REMOVE: project(':loggtjanst:log-messages') - will be integrated into main source
    // REMOVE: project(':loggtjanst:loggtjanst-stub') - will be integrated into main source
    // REMOVE: se.inera.intyg.common dependencies (integration-util, logging-util)
    // REMOVE: se.inera.intyg.infra:monitoring dependency
    
    implementation "com.google.guava:guava:${googleGuavaVersion}"
    implementation "org.apache.activemq:activemq-spring"
    implementation "org.apache.cxf:cxf-rt-features-logging:${apacheCXFVersion}"
    implementation "org.apache.cxf:cxf-rt-frontend-jaxws:${apacheCXFVersion}"
    implementation "org.apache.cxf:cxf-rt-transports-http:${apacheCXFVersion}"
    implementation "codes.rafael.jaxb2_commons:jaxb2-basics:${jaxb2Version}"
    implementation "ch.qos.logback:logback-classic"
    implementation "org.aspectj:aspectjweaver"
    
    // Remove explicit Spring dependencies (managed by Spring Boot)
    // REMOVE: implementation "org.springframework:spring-webmvc"
    // REMOVE: implementation "org.springframework:spring-jms"

    compileOnly "jakarta.servlet:jakarta.servlet-api"
    compileOnly "jakarta.ws.rs:jakarta.ws.rs-api"
    compileOnly "org.apache.geronimo.specs:geronimo-jms_1.1_spec:${geronimoVersion}"
    compileOnly 'org.projectlombok:lombok'

    runtimeOnly "org.glassfish.jaxb:jaxb-runtime"

    // Testing dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation "org.apache.activemq:activemq-broker"
    testImplementation "org.apache.camel:camel-test-spring-junit5:${camelSpringBootVersion}"
    testImplementation "org.awaitility:awaitility"
    // REMOVE: testImplementation "org.junit.jupiter:junit-jupiter-api" (in spring-boot-starter-test)
    // REMOVE: testImplementation "org.mockito:mockito-junit-jupiter" (in spring-boot-starter-test)
    // REMOVE: testImplementation "org.springframework:spring-test" (in spring-boot-starter-test)
    testImplementation "jakarta.xml.bind:jakarta.xml.bind-api"
    // REMOVE: testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine" (in spring-boot-starter-test)

    annotationProcessor 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
}
```

#### Step A.4: Configure JAR packaging

**Action:**
```groovy
bootJar {
    enabled = true
    archiveFileName = 'logsender.jar'
}

jar {
    enabled = false
}

// REMOVE entire gretty configuration block
```

#### Step A.5: Remove gretty tasks

**Action:**
Remove the following from `build.gradle`:
- `gretty { ... }` configuration block
- `tasks.withType(War).configureEach { ... }`
- `gretty "com.mysql:mysql-connector-j"` dependency

Keep `camelTest` task but update:
```groovy
tasks.register('camelTest', Test) {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    include '**/*IT*'
    // REMOVE: jvmArgs = gretty.jvmArgs
}
```

#### Step A.6: Integrate loggtjanst modules into main source

**Background:** The `loggtjanst/log-messages` and `loggtjanst/loggtjanst-stub` folders contain code that should be part of the main logsender application, not separate modules.

**Action:**

1. **Integrate log-messages classes:**
   ```bash
   # Move PDL message models into main source
   cp -r loggtjanst/log-messages/src/main/java/se/inera/intyg/infra/logmessages src/main/java/se/inera/intyg/logsender/model/
   ```
   
   After copying, update package declarations in the moved files from:
   ```java
   package se.inera.intyg.infra.logmessages;
   ```
   to:
   ```java
   package se.inera.intyg.logsender.model;
   ```

2. **Integrate loggtjanst-stub classes:**
   ```bash
   # Move stub service into main source with @Profile("dev")
   cp -r loggtjanst/loggtjanst-stub/src/main/java/* src/main/java/
   ```
   
   Update package as needed and ensure stub classes are annotated with:
   ```java
   @Profile("dev")
   @Component  // or @Service, @Configuration as appropriate
   ```

3. **Remove module folders:**
   ```bash
   # After successful integration and testing
   rm -rf loggtjanst/
   ```

4. **Update settings.gradle:**
   Remove any module includes:
   ```groovy
   // REMOVE: include ':loggtjanst:log-messages'
   // REMOVE: include ':loggtjanst:loggtjanst-stub'
   ```

**Testing:** After integration, verify:
```bash
./gradlew clean build -x test
```
Should compile successfully with integrated classes.

### Dependencies Analysis

**What must be done first:**
- None (this is the foundation phase)

### Testing Strategy

After Phase A changes:
```bash
./gradlew clean build -x test
```

Should compile successfully (tests may fail - we'll fix in Phase G).

### Rollback Plan

If issues arise:
- Revert `build.gradle` changes
- Keep the Git stash of working build file

### OBSERVE Flags


---

## Phase B: Core Configuration - Spring Boot Main Class and XML Removal

### Current State

**Application Bootstrap:** Currently uses `WebApplicationInitializer` pattern:
- `LogSenderWebConfig.java` implements `WebApplicationInitializer`
- Programmatically configures servlet context
- Registers `CXFServlet` and version JSP
- Uses `AnnotationConfigWebApplicationContext`
- Main config class: `LogSenderAppConfig.java`

**XML Configuration Files:**
1. **applicationContext.xml** - Empty beans file (can be removed)
2. **camel-context.xml** - Defines Camel context and endpoints

**Current LogSenderAppConfig:**
```java
@Configuration
@PropertySource("classpath:application.properties")
@PropertySource(ignoreResourceNotFound = true, value = "file:${dev.config.file}")
@Import({LogSenderBeanConfig.class, LogSenderJmsConfig.class, LogSenderWsConfig.class})
@ImportResource(locations = {"classpath:camel-context.xml", "classpath:basic-cache-config.xml", "classpath:/loggtjanst-stub-context.xml"})
public class LogSenderAppConfig {
    // Bean definitions
}
```

**Component Scanning:** Currently none explicit (relies on WebApplicationInitializer)

### Target State

- Create `@SpringBootApplication` main class in `se.inera.intyg.logsender`
- Remove `LogSenderWebConfig.java` (WebApplicationInitializer)
- Remove empty `applicationContext.xml`
- Update `LogSenderAppConfig` to be Spring Boot compatible
- Remove `@ImportResource` for XML files (will migrate Camel in Phase C)
- Enable component scanning from base package

### Migration Steps

#### Step B.1: Create Spring Boot Main Application Class

**Action:** Create new file `src/main/java/se/inera/intyg/logsender/LogsenderApplication.java`:

```java
package se.inera.intyg.logsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot main application class for Logsender.
 * 
 * Logsender is responsible for aggregating and sending PDL log messages
 * to the national logging service using Apache Camel for message routing.
 */
@SpringBootApplication
public class LogsenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogsenderApplication.class, args);
    }
}
```

**Testing:** None yet (app won't start until more migration is done)

#### Step B.2: Update LogSenderAppConfig

**Action:** Modify `src/main/java/se/inera/intyg/logsender/config/LogSenderAppConfig.java`:

```java
package se.inera.intyg.logsender.config;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.apache.cxf.transport.servlet.CXFServlet;

@Configuration
@Import({LogSenderBeanConfig.class, LogSenderJmsConfig.class, LogSenderWsConfig.class})
// REMOVE: @PropertySource annotations (use application.properties)
// REMOVE: @ImportResource (will handle in Phase C)
public class LogSenderAppConfig {

    // REMOVE: propertyConfig() bean (Spring Boot handles this)
    
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setBasenames("version");
        return messageSource;
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public Bus cxf() {
        return new SpringBus();
    }
    
    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServletRegistration() {
        ServletRegistrationBean<CXFServlet> registration = 
            new ServletRegistrationBean<>(new CXFServlet(), "/*");
        registration.setLoadOnStartup(1);
        registration.setName("ws");
        return registration;
    }
}
```

**Note:** CXF servlet registration moved to Spring Boot bean pattern.

#### Step B.3: Remove WebApplicationInitializer

**Action:** Delete file:
- `src/main/java/se/inera/intyg/logsender/webconfig/LogSenderWebConfig.java`

**Rationale:** Spring Boot auto-configuration replaces servlet container initialization.

#### Step B.4: Remove empty XML configuration

**Action:** Delete file:
- `src/main/webapp/WEB-INF/applicationContext.xml`

#### Step B.5: Handle ImportResource dependencies

1. `camel-context.xml` - Will be migrated to Java DSL in Phase C
2. `basic-cache-config.xml` - Need to check if this exists and what it configures
3. `loggtjanst-stub-context.xml` - From infra dependency (being removed)

#### Step B.5: Migrate basic-cache-config.xml to Java configuration

**Current:** `basic-cache-config.xml` exists and configures:
- `@EnableCaching` via `<cache:annotation-driven>`
- Profile-based cache manager:
  - Profiles `caching-enabled` or `prod`: Uses `BasicCacheConfiguration` (from infra - will be replaced)
  - Other profiles: Uses `NoOpCacheManager`

**Action:** Create `src/main/java/se/inera/intyg/logsender/config/CacheConfig.java`:

```java
package se.inera.intyg.logsender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${redis.cache.default_entry_expiry_time_in_seconds:86400}")
    private long defaultEntryExpiryTimeInSeconds;

    /**
     * Redis cache manager for production and when caching is explicitly enabled.
     */
    @Bean
    @Profile({"caching-enabled", "prod"})
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(defaultEntryExpiryTimeInSeconds))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }

    /**
     * No-op cache manager for development and test profiles.
     */
    @Bean
    @Profile("!caching-enabled & !prod")
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
```

**Additional Dependencies:** Add to Phase A Step A.3:
```groovy
dependencies {
    // For Redis caching support
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

**Delete:** `src/main/resources/basic-cache-config.xml` after Java config is created and tested.

**Update:** Remove from `LogSenderAppConfig`:
```java
// REMOVE from @ImportResource: "classpath:basic-cache-config.xml"
```

#### Step B.6: Remove loggtjanst-stub XML import

**Current:** `loggtjanst-stub-context.xml` is imported via `@ImportResource`

**Action:** Since the loggtjanst-stub code has been integrated into the main source in Phase A Step A.6, simply remove the import:

```java
@Configuration
@Import({LogSenderBeanConfig.class, LogSenderJmsConfig.class, LogSenderWsConfig.class})
// REMOVE from @ImportResource: "classpath:/loggtjanst-stub-context.xml"
public class LogSenderAppConfig {
    // ...
}
```

The stub beans are now Spring components in the main application, annotated with `@Profile("dev")` so they only load in development.

**Note:** If the stub had XML configuration, you may need to create equivalent Java `@Configuration` classes with `@Profile("dev")` annotation.

### Dependencies

**Must be done before:**
- Phase A (build configuration)

**Must be done after:**
- Phase C will handle Camel XML migration

### Testing Strategy

After Phase B:
```bash
./gradlew clean build -x test
```

Should still compile. Application won't fully start yet (Camel XML still referenced).

### Rollback Plan

- Restore `LogSenderWebConfig.java`
- Restore `@ImportResource` in `LogSenderAppConfig`
- Remove `LogsenderApplication.java`

### OBSERVE Flags

- (Drop this endpoint) Version JSP (`/version`) currently served by servlet - need Spring Boot equivalent (could be Actuator /info endpoint)

---

## Phase C: Apache Camel Migration - XML to Java DSL and Spring Boot Integration

### Current State

**Camel Configuration:**
- XML-based context in `camel-context.xml`
- Camel Spring XML dependency
- Route builder defined in Java: `LogSenderRouteBuilder.java`
- Endpoints defined in XML, values from properties
- Manual CamelContext configuration

**Current camel-context.xml:**
```xml
<camel:camelContext id="webcertLogMessageSender">
    <camel:routeBuilder ref="logSenderRouteBuilder"/>
    <camel:endpoint id="receiveLogMessageEndpoint" uri="${receiveLogMessageEndpointUri}"/>
    <camel:endpoint id="receiveAggregatedLogMessageEndpoint" uri="${receiveAggregatedLogMessageEndpointUri}"/>
</camel:camelContext>
```

**Current Route Builder:**
- `LogSenderRouteBuilder` extends `RouteBuilder`
- Not currently a Spring component
- Uses `@Value` annotations for configuration
- Well-structured Java DSL routes

**Endpoint URIs (from application.properties):**
```properties
receiveLogMessageEndpointUri=jms:queue:${env.name}.webcert.log.queue
receiveAggregatedLogMessageEndpointUri=jms:queue:${env.name}.webcert.aggregated.log.queue
receiveAggregatedLogMessageDLQUri=jms:queue:DLQ.${env.name}.webcert.aggregated.log.queue
```

### Target State

- Remove `camel-context.xml`
- Remove `camel-spring-xml` dependency
- Make `LogSenderRouteBuilder` a Spring `@Component`
- Use Camel Spring Boot autoconfiguration
- Configure Camel via `application.properties`
- Endpoints defined directly in routes (not in XML)

### Migration Steps

#### Step C.1: Make LogSenderRouteBuilder a Spring Component

**Action:** Modify `src/main/java/se/inera/intyg/logsender/routes/LogSenderRouteBuilder.java`:

```java
package se.inera.intyg.logsender.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import se.inera.intyg.infra.logmessages.PdlLogMessage;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.TemporaryException;

/**
 * Defines the LogSender Camel route which accepts {@link PdlLogMessage} in JSON-serialized TextMessages.
 *
 * @author eriklupander
 */
@Component // ADD THIS
public class LogSenderRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LogSenderRouteBuilder.class);

    @Value("${logsender.bulkSize}")
    private String batchSize;

    @Value("${receiveAggregatedLogMessageEndpointUri}")
    private String newAggregatedLogMessageQueue;

    @Value("${receiveAggregatedLogMessageDLQUri}")
    private String newAggregatedLogMessageDLQ;

    @Value("${logsender.bulkTimeout}")
    private Long batchAggregationTimeout;
    
    @Value("${receiveLogMessageEndpointUri}")
    private String receiveLogMessageEndpointUri; // ADD THIS

    @Override
    public void configure() {
        errorHandler(defaultErrorHandler().logExhausted(false));

        // Route 1: Aggregator route
        // CHANGE: Use URI from property instead of endpoint reference
        from(receiveLogMessageEndpointUri).routeId("aggregatorRoute")
            .split().method("logMessageSplitProcessor")
            .aggregate(new GroupedExchangeAggregationStrategy())
            .constant(true)
            .completionInterval(batchAggregationTimeout)
            .completionPredicate(header("CamelAggregatedSize").isEqualTo(Integer.parseInt(batchSize)))
            .to("bean:logMessageAggregationProcessor")
            .to(newAggregatedLogMessageQueue)
            .stop();

        // Route 2: JMS to Sender route  
        // CHANGE: Use URI from property instead of endpoint reference
        from(newAggregatedLogMessageQueue).routeId("aggregatedJmsToSenderRoute")
            .onException(TemporaryException.class).to("direct:logMessageTemporaryErrorHandlerEndpoint").end()
            .onException(BatchValidationException.class).handled(true).to("direct:logMessageBatchValidationErrorHandlerEndpoint").end()
            .onException(Exception.class).handled(true).to("direct:logMessagePermanentErrorHandlerEndpoint").end()
            .transacted()
            .to("bean:logMessageSendProcessor").stop();

        // Error handling routes (unchanged)
        from("direct:logMessagePermanentErrorHandlerEndpoint").routeId("permanentErrorLogging")
            .log(LoggingLevel.ERROR, LOG,
                simple("ENTER - Permanent exception for LogMessage batch: ${exception.message}\n ${exception.stacktrace}")
                    .toString())
            .stop();

        from("direct:logMessageBatchValidationErrorHandlerEndpoint").routeId("batchValidationErrorLogging")
            .log(LoggingLevel.ERROR, LOG,
                simple("ENTER - Batch validation exception for LogMessage batch: ${exception.message}\n ${exception.stacktrace}")
                    .toString())
            .to(newAggregatedLogMessageDLQ)
            .stop();

        from("direct:logMessageTemporaryErrorHandlerEndpoint").routeId("temporaryErrorLogging")
            .choice()
            .when(header("JMSRedelivered").isEqualTo("false"))
            .log(LoggingLevel.ERROR, LOG,
                simple("ENTER - Temporary exception for logMessage batch: ${exception.message}\n ${exception.stacktrace}")
                    .toString())
            .otherwise()
            .log(LoggingLevel.WARN, LOG,
                simple("ENTER - Temporary exception (redelivered) for logMessage batch: ${exception.message}").toString())
            .stop();
    }
}
```

**Key changes:**
1. Added `@Component` annotation
2. Added `@Value("${receiveLogMessageEndpointUri}")` property
3. Changed `from("receiveLogMessageEndpoint")` to `from(receiveLogMessageEndpointUri)`
4. Changed `from("receiveAggregatedLogMessageEndpoint")` to `from(newAggregatedLogMessageQueue)`

#### Step C.2: Add Camel Spring Boot properties

**Action:** Add to `src/main/resources/application.properties`:

```properties
# Camel Spring Boot configuration
camel.springboot.name=logsender-camel-context
camel.springboot.main-run-controller=true
camel.springboot.jmx-enabled=true

# Keep existing Camel-related properties
# (receiveLogMessageEndpointUri, etc. already exist)
```

#### Step C.3: Remove camel-context.xml

**Action:** Delete file:
- `src/main/resources/camel-context.xml`

#### Step C.4: Remove Camel XML dependency

**Action:** In `build.gradle`, remove:
```groovy
// REMOVE: runtimeOnly "org.apache.camel:camel-spring-xml:${camelVersion}"
```

This dependency is no longer needed with Camel Spring Boot and Java DSL.

#### Step C.5: Verify processor beans exist

**OBSERVE:** The routes reference these processors as beans:
- `logMessageSplitProcessor`
- `logMessageAggregationProcessor`
- `logMessageSendProcessor`

These should be defined in `LogSenderBeanConfig.java`. Verify they are Spring beans with correct names.

### Dependencies

**Must be done before:**
- Phase A (Camel Spring Boot starters added)
- Phase B (Spring Boot main class exists)

### Testing Strategy

After Phase C:
```bash
./gradlew clean bootRun
```

Application should start with Camel routes loaded. Check logs for:
```
Routes started: aggregatorRoute, aggregatedJmsToSenderRoute, permanentErrorLogging, batchValidationErrorLogging, temporaryErrorLogging
```

### Rollback Plan

- Restore `camel-context.xml`
- Remove `@Component` from `LogSenderRouteBuilder`
- Restore endpoint references in routes
- Add back `camel-spring-xml` dependency

### OBSERVE Flags

- ⚠️ **OBSERVE:** Verify that `logMessageSplitProcessor`, `logMessageAggregationProcessor`, and `logMessageSendProcessor` beans are properly defined in `LogSenderBeanConfig`
- ⚠️ **OBSERVE:** Test that ActiveMQ connection and message routing works correctly after migration

---

## Phase D: Remove Common/Infra Dependencies

### Current State

**Dependencies to Remove:**
```groovy
implementation "se.inera.intyg.common:integration-util:${commonVersion}"
implementation "se.inera.intyg.common:logging-util:${commonVersion}"
implementation "se.inera.intyg.infra:log-messages:${infraVersion}"
implementation "se.inera.intyg.infra:loggtjanst-stub:${infraVersion}"
implementation "se.inera.intyg.infra:monitoring:${infraVersion}"
```

**Usage Analysis Needed:**
- Check what classes are imported from these dependencies
- Identify what functionality they provide
- Find Spring Boot equivalents or create custom implementations

### Target State

- All common/infra dependencies removed
- Functionality replaced with:
  - Spring Boot built-in features
  - Custom implementations where needed
  - Actuator for monitoring

### Migration Steps

#### Step D.1: Analyze common/infra usage

**Action:** Search codebase for imports from these packages:
```bash
# Find all imports from common/infra
grep -r "import se.inera.intyg.common" src/
grep -r "import se.inera.intyg.infra" src/
```

**OBSERVE:** Need to identify:
1. Which classes are used from common/infra
2. What functionality they provide
3. How to replace them

Common replacements:
- `integration-util` → Spring RestTemplate/RestClient or remove if not used
- `logging-util` → Logback + MDC (already have `MdcHelper`)
- `monitoring` → Spring Boot Actuator

**Note:** PDL message models and stub service code have been **integrated directly into the main source** in Phase A Step A.6, so they are now part of the logsender application packages.

#### Step D.2: Replace monitoring with Actuator

**Action:** Already added `spring-boot-starter-actuator` in Phase A.

Configure in `application.properties`:
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,metrics,info,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
```

#### Step D.3: Remove external infra/common dependencies from build.gradle

**Action:** After ensuring replacements are in place:
```groovy
// REMOVE these external dependency lines:
// implementation "se.inera.intyg.common:integration-util:${commonVersion}"
// implementation "se.inera.intyg.common:logging-util:${commonVersion}"
// implementation "se.inera.intyg.infra:monitoring:${infraVersion}"

// KEEP or EVALUATE:
// implementation "se.inera.intyg.infra:log-messages:${infraVersion}"
// (Only if it contains PDL message models)
```

### Dependencies

**Must be done before:**
- Phase A, B, C (Spring Boot foundation in place)

### Testing Strategy

After Phase D:
```bash
./gradlew clean build
```

Check for compilation errors related to missing classes. Replace with Spring Boot equivalents.

### Rollback Plan

- Add back common/infra dependencies
- Restore import statements

### OBSERVE Flags

- ⚠️ **OBSERVE:** Identify all classes used from `se.inera.intyg.common.integration-util` and `se.inera.intyg.common.logging-util` packages to determine what needs replacement
- ⚠️ **OBSERVE:** Verify `monitoring` features from infra are adequately replaced by Actuator (health checks, metrics)

---

## Phase E: Logging Migration - ECS Structured Logging

### Current State

**Logging Setup:**
- Logback already in use (`ch.qos.logback:logback-classic`)
- ECS encoder already in dependencies: `co.elastic.logging:logback-ecs-encoder:1.6.0`
- Logback configuration from infra monitoring: `LogbackConfiguratorContextListener`
- External logback file: `logback.file` system property
- MDC helper exists: `MdcHelper.java` (generates trace/span IDs)
- Performance logging AOP: `PerformanceLoggingAdvice.java`

**Current Logback Configuration:**
- Referenced in `LogSenderWebConfig`: `logbackConfigParameter = "logback.file"`
- Development config: `-Dlogback.file=${applicationDir}/config/logback-spring.xml`
- Uses external XML file from devops folder

### Target State

- Use Spring Boot's logback integration (`logback-spring.xml`)
- Structured ECS logging enabled
- MDC filter as Spring component
- Performance logging with AOP
- Logging configuration in standard location

### Migration Steps

#### Step E.1: Create Spring Boot logback-spring.xml

**Action:** Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProperty scope="context" name="application.name" source="spring.application.name" defaultValue="logsender"/>
    
    <!-- Console appender with ECS format -->
    <appender name="CONSOLE_ECS" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="co.elastic.logging.logback.EcsEncoder">
            <serviceName>${application.name}</serviceName>
        </encoder>
    </appender>
    
    <!-- File appender (optional, for production) -->
    <appender name="FILE_ECS" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/logsender.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/logsender.%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="co.elastic.logging.logback.EcsEncoder">
            <serviceName>${application.name}</serviceName>
        </encoder>
    </appender>
    
    <!-- Spring Boot profile-specific configuration -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE_ECS"/>
        </root>
        <logger name="se.inera.intyg.logsender" level="DEBUG"/>
        <logger name="org.apache.camel" level="INFO"/>
    </springProfile>
    
    <springProfile name="!dev">
        <root level="INFO">
            <appender-ref ref="FILE_ECS"/>
        </root>
        <logger name="se.inera.intyg.logsender" level="INFO"/>
    </springProfile>
</configuration>
```

#### Step E.2: Add Spring application name

**Action:** Add to `application.properties`:
```properties
spring.application.name=logsender

# Logging configuration
logging.level.se.inera.intyg.logsender=DEBUG
logging.level.org.apache.camel=INFO
logging.level.org.springframework=INFO
```

#### Step E.3: Create MDC Filter Component

**Action:** Create `src/main/java/se/inera/intyg/logsender/logging/MdcLoggingFilter.java`:

```java
package se.inera.intyg.logsender.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that adds trace and span IDs to MDC for structured logging.
 * These IDs are propagated through the application for correlation.
 */
@Component
@RequiredArgsConstructor
public class MdcLoggingFilter implements Filter {

    private static final String TRACE_ID_KEY = "trace.id";
    private static final String SPAN_ID_KEY = "span.id";
    private static final String SESSION_ID_KEY = "session.id";

    private final MdcHelper mdcHelper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        try {
            // Generate or extract trace/span IDs
            String traceId = extractOrGenerateTraceId((HttpServletRequest) request);
            String spanId = mdcHelper.spanId();
            
            // Add to MDC
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);
            
            // Add session ID if present
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if (httpRequest.getSession(false) != null) {
                MDC.put(SESSION_ID_KEY, httpRequest.getSession().getId());
            }
            
            chain.doFilter(request, response);
        } finally {
            // Always clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        // Check for existing trace ID in header (distributed tracing)
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = mdcHelper.traceId();
        }
        return traceId;
    }
}
```

#### Step E.4: Verify Performance Logging AOP

**Action:** Check `src/main/java/se/inera/intyg/logsender/logging/PerformanceLoggingAdvice.java`:

Ensure it's marked as `@Component` and `@Aspect` (it already is):
```java
@Component
@Aspect
public class PerformanceLoggingAdvice {
    // Existing code should work with Spring Boot AOP
}
```

Spring Boot AOP starter (added in Phase A) will auto-configure AspectJ.

#### Step E.5: Remove old logback configuration references

**Action:** Already done in Phase B when we removed `LogSenderWebConfig`.

Spring Boot will automatically use `logback-spring.xml` from resources.

#### Step E.6: Update application properties for structured logging

**Action:** Add to `application.properties`:
```properties
# Structured logging configuration
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Dependencies

**Must be done before:**
- Phase A (ECS encoder dependency added, AOP starter added)
- Phase B (Spring Boot configuration)

### Testing Strategy

After Phase E:
```bash
./gradlew clean bootRun
```

Check console output for JSON-formatted ECS logs. Should see fields like:
- `@timestamp`
- `log.level`
- `message`
- `trace.id`
- `span.id`
- `service.name: "logsender"`

### Rollback Plan

- Remove `logback-spring.xml`
- Restore old logback configuration approach
- Remove `MdcLoggingFilter`

### OBSERVE Flags

- ⚠️ **OBSERVE:** Verify ECS log format includes all required fields for log aggregation
- ⚠️ **OBSERVE:** Check if existing `devops/dev/config/logback-spring.xml` has custom configuration that should be preserved
- ⚠️ **OBSERVE:** Ensure MDC context propagates through Camel routes correctly

---

## Phase F: Configuration Cleanup and Property Consolidation

### Current State

**Property Sources:**
- `application.properties` in `src/main/resources`
- External dev config: `file:${dev.config.file}` (from `@PropertySource`)
- Environment-specific properties in `devops/dev/config/application-dev.properties`
- System properties passed via JVM args (Gretty configuration)

**Current Properties in application.properties:**
- ActiveMQ broker configuration
- JMS queue names
- Loggtjanst endpoint configuration
- Aggregation configuration
- Certificate configuration
- Redis cache configuration

### Target State

- Primary: `application.properties` for common configuration
- Profile-specific: `application-dev.properties` for development
- Spring Boot property conventions
- Externalized sensitive values
- Remove Gretty-specific properties

### Migration Steps

#### Step F.1: Reorganize application.properties

**Action:** Update `src/main/resources/application.properties`:

```properties
# Application configuration
spring.application.name=logsender
env.name=${ENV_NAME:}

# Server configuration (Spring Boot embedded Tomcat)
server.port=${SERVER_PORT:8080}

# ActiveMQ Broker Configuration
spring.activemq.broker-url=${ACTIVEMQ_BROKER_URL:tcp://localhost:61616}
spring.activemq.user=${ACTIVEMQ_USERNAME:}
spring.activemq.password=${ACTIVEMQ_PASSWORD:}
spring.activemq.non-blocking-redelivery=true

# JMS Queue Configuration
logsender.queue.receive=${env.name}.webcert.log.queue
logsender.queue.aggregated=${env.name}.webcert.aggregated.log.queue
logsender.queue.dlq=DLQ.${env.name}.webcert.aggregated.log.queue

# Camel endpoint URIs (for backward compatibility)
receiveLogMessageEndpointUri=jms:queue:${logsender.queue.receive}
receiveAggregatedLogMessageEndpointUri=jms:queue:${logsender.queue.aggregated}
receiveAggregatedLogMessageDLQUri=jms:queue:${logsender.queue.dlq}

# Message Aggregation Configuration
logsender.bulkSize=10
logsender.bulkTimeout=60000

# Loggtjanst Service Configuration
loggtjanst.base.url=${LOGGTJANST_BASE_URL:}
loggtjanst.logical-address=${LOGGTJANST_LOGICAL_ADDRESS:}
loggtjanst.endpoint.url=${loggtjanst.base.url}/informationsecurity/auditing/log/StoreLog/v2/rivtabp21

# Certificate Configuration
logsender.certificate.file=${CERTIFICATE_FILE:}
logsender.certificate.type=JKS
logsender.certificate.password=${CERTIFICATE_PASSWORD:}
logsender.truststore.file=${TRUSTSTORE_FILE:}
logsender.truststore.type=JKS
logsender.truststore.password=${TRUSTSTORE_PASSWORD:}
logsender.key-manager.password=${KEY_MANAGER_PASSWORD:}

# Redis Cache Configuration (if using Spring Data Redis)
spring.data.redis.host=${REDIS_HOST:127.0.0.1}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
redis.cache.default_entry_expiry_time_in_seconds=86400
redis.sentinel.master.name=master

# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.info.env.enabled=true

# Logging
logging.level.se.inera.intyg.logsender=DEBUG
logging.level.org.apache.camel=INFO
logging.level.org.springframework=INFO
logging.level.org.apache.activemq=WARN

# Camel Configuration
camel.springboot.name=logsender-camel-context
camel.springboot.main-run-controller=true
camel.springboot.jmx-enabled=true
```

#### Step F.2: Create application-dev.properties

**Action:** Create `src/main/resources/application-dev.properties`:

```properties
# Development profile configuration
env.name=

# Development server port
server.port=8010

# Development ActiveMQ (use embedded or local)
spring.activemq.broker-url=tcp://localhost:61616

# Development Loggtjanst (stub)
loggtjanst.base.url=http://localhost:8020
loggtjanst.logical-address=SE165565594230-1000

# Development certificates (from devops folder)
logsender.certificate.file=${application.dir:./devops/dev}/certifikat/certificate.jks
logsender.truststore.file=${application.dir:./devops/dev}/certifikat/truststore.jks

# Development logging
logging.level.se.inera.intyg.logsender=DEBUG
logging.level.org.apache.camel=DEBUG
```

#### Step F.3: Update JMS configuration to use Spring Boot properties

**Action:** Modify `LogSenderJmsConfig.java`:

```java
@Configuration
@EnableTransactionManagement
public class LogSenderJmsConfig {

    // REPLACE @Value annotations with Spring Boot properties
    @Value("${spring.activemq.user:}")
    private String activemqBrokerUsername;

    @Value("${spring.activemq.password:}")
    private String activemqBrokerPassword;

    @Value("${spring.activemq.broker-url}")
    private String activemqBrokerUrl;

    // Rest of configuration remains the same
    // ...
}
```

**OBSERVE:** Consider using Spring Boot's ActiveMQ autoconfiguration instead of manual configuration. This would simplify the JMS setup significantly.

#### Step F.4: Use @ConfigurationProperties for grouped settings

**Action:** Create `src/main/java/se/inera/intyg/logsender/config/LogsenderProperties.java`:

```java
package se.inera.intyg.logsender.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "logsender")
@Validated
@Data
public class LogsenderProperties {

    /**
     * Number of messages to aggregate before sending.
     */
    @Positive
    private int bulkSize = 10;

    /**
     * Timeout in milliseconds for message aggregation.
     */
    @Positive
    private long bulkTimeout = 60000;

    /**
     * Queue configuration.
     */
    private Queue queue = new Queue();

    /**
     * Certificate configuration.
     */
    private Certificate certificate = new Certificate();

    @Data
    public static class Queue {
        private String receive;
        private String aggregated;
        private String dlq;
    }

    @Data
    public static class Certificate {
        @NotBlank
        private String file;
        private String type = "JKS";
        @NotBlank
        private String password;
        private String keyManagerPassword;
    }
}
```

This provides type-safe configuration and validation.

#### Step F.5: Remove Gretty-specific properties

**Action:** Already done in Phase A when Gretty configuration was removed.

### Dependencies

**Must be done before:**
- Phase A, B, C, D, E (Spring Boot foundation)

### Testing Strategy

After Phase F:
```bash
./gradlew clean bootRun --args='--spring.profiles.active=dev'
```

Verify:
- Properties load correctly
- Application starts
- Configuration values are injected properly

### Rollback Plan

- Restore original `application.properties`
- Remove `LogsenderProperties`
- Keep old `@Value` annotations

### OBSERVE Flags

- ⚠️ **OBSERVE:** Check if `basic-cache-config.xml` defines Redis configuration that needs to be migrated to properties
- ⚠️ **OBSERVE:** Verify certificate file paths work in different environments (dev/test/prod)
- ⚠️ **OBSERVE:** Consider using Spring Boot ActiveMQ autoconfiguration instead of manual JMS config

---

## Phase G: Testing Updates - Spring Boot Test Framework

### Current State

**Test Framework:**
- JUnit 5 (Jupiter)
- Mockito
- Spring Test
- Camel Test Spring JUnit5
- Awaitility
- ActiveMQ Broker for integration tests

**Test Structure:**
- Unit tests excluded from integration test pattern: `exclude '**/*IT*'`
- Integration tests: `include '**/*IT*'`
- Separate `camelTest` task

**Current Issues:**
- Tests reference Gretty JVM args (removed in Phase A)
- May use old Spring test annotations

### Target State

- Use `@SpringBootTest` for integration tests
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Spring Boot test autoconfiguration
- Camel Spring Boot test support
- Random port for integration tests

### Migration Steps

#### Step G.1: Update test dependencies

**Action:** Already done in Phase A:
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

This includes JUnit 5, Mockito, Spring Test, AssertJ, and more.

#### Step G.2: Update integration tests to use @SpringBootTest

**Action:** Find integration tests and update:

```java
// BEFORE
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LogSenderAppConfig.class)
public class SomeIntegrationTest {
    // ...
}

// AFTER
@SpringBootTest(
    classes = LogsenderApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class SomeIntegrationTest {
    // ...
}
```

#### Step G.3: Create test profile configuration

**Action:** Create `src/test/resources/application-test.properties`:

```properties
# Test profile configuration
spring.application.name=logsender-test
env.name=test

# Embedded ActiveMQ for tests
spring.activemq.broker-url=vm://localhost?broker.persistent=false

# Disable external services
loggtjanst.base.url=http://localhost:9999
loggtjanst.logical-address=TEST

# Fast aggregation for tests
logsender.bulkSize=2
logsender.bulkTimeout=1000

# Test logging
logging.level.se.inera.intyg.logsender=DEBUG
logging.level.org.apache.camel=INFO
```

#### Step G.4: Update Camel integration tests

**Action:** Ensure Camel tests use Spring Boot test support:

```java
@SpringBootTest
@ActiveProfiles("test")
public class LogSenderRouteBuilderIT {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    public void testAggregatorRoute() throws Exception {
        // Test logic
    }
}
```

#### Step G.5: Update test task in build.gradle

**Action:** Update test tasks:

```groovy
test {
    useJUnitPlatform()
    exclude '**/*IT*'
    // REMOVE: jvmArgs = gretty.jvmArgs
}

tasks.register('integrationTest', Test) {
    description = 'Runs integration tests'
    group = 'verification'
    
    useJUnitPlatform()
    include '**/*IT*'
    
    // Run after unit tests
    shouldRunAfter test
}

tasks.register('camelTest', Test) {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    include '**/*IT*'
    // REMOVED: jvmArgs = gretty.jvmArgs
}
```

#### Step G.6: Mock external services in tests

**Action:** Create test configuration for mocking external services:

```java
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public StoreLogService mockStoreLogService() {
        return Mockito.mock(StoreLogService.class);
    }
}
```

### Dependencies

**Must be done before:**
- Phase A (test dependencies added)
- All other phases (application fully migrated)

### Testing Strategy

After Phase G:
```bash
./gradlew clean test
./gradlew integrationTest
```

All tests should pass.

### Rollback Plan

- Restore old test annotations
- Restore Gretty JVM args in test tasks

### OBSERVE Flags

- ⚠️ **OBSERVE:** Review all existing tests and update to Spring Boot test patterns
- ⚠️ **OBSERVE:** Ensure embedded ActiveMQ works correctly for integration tests
- ⚠️ **OBSERVE:** Check if any tests depend on external services that need mocking

---

## Phase H: Packaging and Deployment - WAR to JAR Conversion

### Current State

**Packaging:**
- WAR file deployment
- External Tomcat server (via Gretty for dev)
- `src/main/webapp` directory structure
- `web.xml` and servlet configuration
- JSP files (`version.jsp`)

**Deployment:**
- Gretty for local development
- Dockerfile (may reference WAR deployment)
- Application server configuration

### Target State

- Executable JAR with embedded Tomcat
- No `src/main/webapp` needed
- Spring Boot DevTools for local development
- Updated Dockerfile for JAR
- JSP replaced with REST endpoint or Actuator

### Migration Steps

#### Step H.1: Configure Spring Boot JAR packaging

**Action:** Already done in Phase A:
```groovy
bootJar {
    enabled = true
    archiveFileName = 'logsender.jar'
}

jar {
    enabled = false
}
```

#### Step H.2: Handle version.jsp

**Current:** `src/main/webapp/version.jsp` served at `/version`

**Action:** Replace with Actuator `/info` endpoint.

Update `application.properties`:
```properties
# Info endpoint configuration
management.info.build.enabled=true
management.info.git.enabled=true
management.info.env.enabled=true

info.app.name=logsender
info.app.description=PDL Log Message Sender
info.app.version=@project.version@
```

Add `src/main/resources/git.properties` (auto-generated by Git plugin):
```groovy
// In build.gradle
plugins {
    id "com.gorylenko.gradle-git-properties" version "2.4.1"
}
```

**Testing:** Access `http://localhost:8080/actuator/info`

#### Step H.3: Remove webapp directory

**Action:** After confirming no other files in `src/main/webapp` are needed:
- Delete `src/main/webapp/WEB-INF/` directory
- Delete `src/main/webapp/version.jsp`
- Keep `src/main/webapp` only if static resources are needed (move to `src/main/resources/static`)

#### Step H.4: Update Dockerfile

**Current Dockerfile (assumed):**
```dockerfile
FROM tomcat:10-jdk21
COPY build/libs/*.war /usr/local/tomcat/webapps/logsender.war
```

**New Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the Spring Boot JAR
COPY build/libs/logsender.jar app.jar

# Expose port
EXPOSE 8080

# Health check using Actuator
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Step H.5: Update run configuration

**Development:**
Replace Gretty with Spring Boot DevTools:

```groovy
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

**Run application:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### Step H.6: Update build scripts

**Action:** If there are any deployment scripts referencing WAR files, update them to use JAR.

### Dependencies

**Must be done before:**
- All previous phases (application fully migrated to Spring Boot)

### Testing Strategy

After Phase H:
```bash
# Build JAR
./gradlew clean bootJar

# Run JAR
java -jar build/libs/logsender.jar --spring.profiles.active=dev

# Verify endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info

# Build Docker image
docker build -t logsender:latest .

# Run Docker container
docker run -p 8080:8080 logsender:latest
```

### Rollback Plan

- Restore WAR packaging
- Restore Gretty configuration
- Restore Dockerfile for WAR deployment

### OBSERVE Flags

- ⚠️ **OBSERVE:** Check if there are static resources in `src/main/webapp` that need migration
- ⚠️ **OBSERVE:** Verify Dockerfile base image and any environment-specific configurations
- ⚠️ **OBSERVE:** Update deployment documentation and CI/CD pipelines for JAR deployment

---

## Summary of Migration Phases

| Phase | Focus | Complexity | Dependencies | Testing |
|-------|-------|------------|--------------|---------|
| A | Build & Dependencies | Medium | None | Compile |
| B | Core Config & Main Class | Medium | A | Compile |
| C | Camel Migration | Medium | A, B | Runtime |
| D | Remove Common/Infra | High | A, B, C | Compile/Runtime |
| E | Logging (ECS) | Low | A, B | Runtime |
| F | Properties Cleanup | Low | All above | Runtime |
| G | Testing Updates | Medium | All above | All tests |
| H | JAR Packaging | Low | All above | Full deployment |

---

## Critical OBSERVE Items Summary

### High Priority

1. **Common/Infra dependency usage** (Phase D) - Identify all classes used from integration-util and logging-util packages to determine what needs replacement
2. **Processor beans** (Phase C) - Verify logMessageSplitProcessor, logMessageAggregationProcessor, and logMessageSendProcessor exist in LogSenderBeanConfig

### Medium Priority

3. **loggtjanst-stub XML configuration** (Phase B) - Check if stub had XML config files that need Java equivalents with @Profile("dev")
4. **ActiveMQ configuration** (Phase F) - Consider using Spring Boot autoconfiguration instead of manual JMS config
5. **Certificate paths** (Phase F) - Verify certificate file paths work in all environments (dev/test/prod)
6. **External service mocking** (Phase G) - Ensure tests can run independently with proper mocks
7. **Static resources** (Phase H) - Check webapp directory for any static resources that need migration to src/main/resources/static

---

## Success Criteria

Migration is successful when:
- ✅ Application builds without errors
- ✅ Application starts as Spring Boot JAR
- ✅ Camel routes load and process messages
- ✅ ActiveMQ connectivity works
- ✅ Logs are in ECS structured format
- ✅ All unit tests pass
- ✅ All integration tests pass
- ✅ Actuator endpoints respond
- ✅ Docker image builds and runs
- ✅ No common/infra dependencies remain
- ✅ Application runs with `--spring.profiles.active=dev`

---

**End of Migration Guide**

This guide provides a complete, phased approach to migrating logsender from Spring Framework to Spring Boot 3.5.8. Each phase can be completed incrementally, tested, and verified before moving to the next phase.

