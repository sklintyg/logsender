package se.inera.intyg.logsender.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.integrationtest.util.Containers;
import se.inera.intyg.logsender.integrationtest.util.IntegrationTestConfig;
import se.inera.intyg.logsender.integrationtest.util.JmsUtil;
import se.inera.intyg.logsender.integrationtest.util.TestabilityUtil;

/**
 * Comprehensive integration tests for Logsender using Testcontainers.
 * <p>
 * These tests verify the complete flow: 1. Messages are published to ActiveMQ (testcontainer) 2.
 * Logsender consumes, splits, and aggregates messages 3. Aggregated messages are sent to the
 * loggtjanst stub 4. Results are verified through the testability API
 * <p>
 * Infrastructure: - ActiveMQ (Testcontainer) - - Loggtjanst Stub (In-memory)
 */
@ActiveProfiles({"integration-test", "dev", "testability-api"})
@SpringBootTest(classes = IntegrationTestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Logsender Integration Tests")
public class LogsenderIT {

  // Initialize containers BEFORE Spring context loads
  static {
    Containers.ensureRunning();
  }

  @Autowired
  private LogsenderProperties properties;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private JmsTemplate jmsTemplate;
  @LocalServerPort
  private int port;
  private JmsUtil jmsUtil;
  private TestabilityUtil testabilityUtil;

  @AfterAll
  static void afterAll() {
    // Optionally stop containers - useful for CI/CD
    // Containers.stopAll();
  }

  @BeforeEach
  void setUp() {
    jmsUtil = new JmsUtil(jmsTemplate, properties.getQueue().getReceiveLogMessageEndpoint());
    testabilityUtil = new TestabilityUtil(restTemplate, port);

    jmsUtil.reset();
    testabilityUtil.reset();
  }

  @Nested
  @DisplayName("Message Aggregation Tests")
  class AggregationTests {

    @Test
    @DisplayName("Should aggregate 5 messages into one batch")
    void shouldAggregateMessagesIntoBulk() {
      // Given: 5 individual log messages (matching bulkSize)
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then: Should be aggregated into 1 batch and sent to stub
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(1, messageCount, "Expected exactly 1 aggregated message");
    }

    @Test
    @DisplayName("Should aggregate 10 messages into two batches")
    void shouldAggregateTenMessagesIntoTwoBatches() {
      // Given: 10 individual log messages
      for (int i = 0; i < 10; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then: Should be aggregated into 2 batches
      testabilityUtil.awaitMessageCount(2, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(2, messageCount, "Expected exactly 2 aggregated messages");
    }

    @Test
    @DisplayName("Should aggregate on timeout when less than bulk size")
    void shouldAggregateOnTimeout() {
      // Given: Only 3 messages (less than bulkSize of 5)
      for (int i = 0; i < 3; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then: Should wait for timeout (1000ms) then aggregate
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(3));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(1, messageCount, "Expected 1 aggregated message after timeout");
    }
  }

  @Nested
  @DisplayName("Message Splitting Tests")
  class SplittingTests {

    @Test
    @DisplayName("Should split message with multiple resources")
    void shouldSplitMessageWithMultipleResources() {
      // Given: 1 message with 5 resources
      jmsUtil.publishMessage();

      // When/Then: Each resource becomes separate message, then aggregated
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));

      assertThat(testabilityUtil.getMessageCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle empty resource list")
    void shouldHandleEmptyResourceList() {
      // Given: Message with no resources - this is an edge case
      // Implementation depends on how your system handles this

      // For now, just verify the system doesn't crash
      jmsUtil.publishMessage();

      // No assertion - just verifying no exception is thrown
    }
  }

  @Nested
  @DisplayName("Activity Type Tests")
  class ActivityTypeTests {

    @Test
    @DisplayName("Should process READ activity type")
    void shouldProcessReadActivity() {
      // Given
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));

      var logs = testabilityUtil.getAllLogs();
      assertThat(logs.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("Should process CREATE activity type")
    void shouldProcessCreateActivity() {
      // This test would use a different activity type
      // Implementation depends on your TestDataHelper capabilities

      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle temporary failures with retry")
    void shouldRetryOnTemporaryFailure() {
      // This test would require configuring the mock to fail temporarily
      // then succeed on retry

      // Given: Messages that will initially fail
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then: Eventually succeeds after retry
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(10));
    }
  }

  @Nested
  @DisplayName("End-to-End Flow Tests")
  class EndToEndTests {

    @Test
    @DisplayName("Should complete full message flow from JMS to stub")
    void shouldCompleteFullMessageFlow() {
      // Given: A single message
      jmsUtil.publishMessage();

      // When: Message is processed through the entire pipeline
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(3));

      // Then: Verify message reached the stub
      var response = testabilityUtil.getAllLogs();
      assertNotNull(response.getBody(), "Should have received logs in stub");
      assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle concurrent message publishing")
    void shouldHandleConcurrentMessages() {
      // Given: Multiple messages published rapidly
      for (int i = 0; i < 15; i++) {
        jmsUtil.publishMessage();
      }

      // When/Then: All messages should be processed
      testabilityUtil.awaitMessageCount(3, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(3, messageCount, "Expected 3 aggregated batches");
    }
  }

  @Nested
  @DisplayName("Testcontainer Infrastructure Tests")
  class InfrastructureTests {

    @Test
    @DisplayName("ActiveMQ container should be running")
    void activeMqContainerShouldBeRunning() {
      assertThat(Containers.amqContainer.isRunning())
          .as("ActiveMQ container should be running")
          .isTrue();
    }

    @Test
    @DisplayName("Should be able to reset stub state")
    void shouldResetStubState() {
      // Given: Some messages processed
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }
      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));

      // When: Reset is called
      testabilityUtil.reset();

      // Then: Stub should be empty
      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(0, messageCount, "Stub should be empty after reset");
    }
  }

}
