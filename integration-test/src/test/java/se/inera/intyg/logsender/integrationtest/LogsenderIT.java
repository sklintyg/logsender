package se.inera.intyg.logsender.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.inera.intyg.logsender.integrationtest.helper.TestDataHelper.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
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
import se.inera.intyg.logsender.integrationtest.helper.TestDataHelper;
import se.inera.intyg.logsender.integrationtest.helper.ValueInclude;
import se.inera.intyg.logsender.integrationtest.util.Containers;
import se.inera.intyg.logsender.integrationtest.util.JmsUtil;
import se.inera.intyg.logsender.integrationtest.util.TestabilityUtil;
import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.PdlLogMessage;

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
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Logsender Integration Tests")
public class LogsenderIT {

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
    Containers.stopAll();
  }

  @BeforeEach
  void setUp() {
    jmsUtil = new JmsUtil(jmsTemplate, properties);
    testabilityUtil = new TestabilityUtil(restTemplate, port);

    jmsUtil.reset();
    testabilityUtil.reset();
  }


  private String buildPdlLogMessageWithInvalidResourceJson(int resources) throws IOException {
    String bodyOfSix = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ, resources);
    ObjectNode jsonNode = (ObjectNode) new CustomObjectMapper().readTree(bodyOfSix);
    ArrayNode pdlResourceList = (ArrayNode) jsonNode.get("pdlResourceList");

    JsonNode invalidJsonNode = new TextNode("Some text that doesn't belong here");
    ObjectNode resourceNode = (ObjectNode) pdlResourceList.get(2);
    resourceNode.set("resourceOwner", invalidJsonNode);

    return OBJECT_MAPPER.writeValueAsString(jsonNode);
  }

  @Nested
  @DisplayName("Message Aggregation Tests")
  class AggregationTests {

    @Test
    @DisplayName("Should aggregate 6 messages into one batch")
    void shouldAggregateMessagesIntoBulk() {
      for (int i = 0; i < 6; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitBatchCount(1, Duration.ofSeconds(5));

      int batchCount = testabilityUtil.getBatchCount();
      assertEquals(1, batchCount, "Expected exactly 1 aggregated message");
      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(5, messageCount, "Expected 10 individual log entries");
    }

    @Test
    @DisplayName("Should aggregate 10 messages into two batches")
    void shouldAggregateTenMessagesIntoTwoBatches() {
      for (int i = 0; i < 10; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitBatchCount(2, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(10, messageCount, "Expected 10 individual log entries");

      int batchCount = testabilityUtil.getBatchCount();
      assertEquals(2, batchCount, "Expected messages to be sent in 2 batches");
    }

    @Test
    @DisplayName("Should aggregate on timeout when less than bulk size")
    void shouldAggregateOnTimeout() {
      // Given: Only 3 messages (less than bulkSize of 5)
      for (int i = 0; i < 3; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitBatchCount(1, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(3, messageCount, "Expected 3 individual log entries");

      int batchCount = testabilityUtil.getBatchCount();
      assertEquals(1, batchCount, "Expected messages to be sent in 1 batch after timeout");
    }
  }

  @Nested
  @DisplayName("Message Splitting Tests")
  class SplittingTests {

    @Test
    @DisplayName("Should split message with multiple resources")
    void shouldSplitMessageWithMultipleResources() {
      jmsUtil.publishMessage();

      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(5));

      assertThat(testabilityUtil.getMessageCount()).isGreaterThan(0);
    }

  }

  @Nested
  @DisplayName("Activity Type Tests")
  class ActivityTypeTests {

    @Test
    @DisplayName("Should process READ activity type")
    void shouldProcessReadActivity() {
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitMessageCount(5, Duration.ofSeconds(5));
      testabilityUtil.awaitBatchCount(1, Duration.ofSeconds(5));

      var logs = testabilityUtil.getAllLogs();
      assertThat(logs.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("Should process CREATE activity type")
    void shouldProcessCreateActivity() {

      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage(ActivityType.CREATE);
      }

      testabilityUtil.awaitMessageCount(5, Duration.ofSeconds(5));
      testabilityUtil.awaitBatchCount(1, Duration.ofSeconds(5));
      var logs = testabilityUtil.getAllLogs();
      assertThat(logs.getBody()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    void ensureStubReceivesNoMessageAfterOneWithSixWhereOnIsInvalidHasBeenSent()
        throws IOException {
      String body = buildPdlLogMessageWithInvalidResourceJson(6);

      jmsUtil.publishMessage(body);

      testabilityUtil.awaitMessageCount(0, Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Should handle stub offline - TemporaryException messages are logged and discarded")
    void shouldHandleStubOfflineWithTemporaryException() throws InterruptedException {
      testabilityUtil.setStubOffline();

      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      Thread.sleep(3000);

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(0, messageCount, "No messages should be stored when stub is offline");

      int dlqCount = jmsUtil.numberOfDLQMessages();
      assertEquals(0, dlqCount,
          "TemporaryException messages are logged and discarded, not sent to DLQ");

    }

    @Test
    @DisplayName("Should handle validation errors and send to DLQ")
    void shouldHandleValidationErrors() {
      testabilityUtil.setErrorState("VALIDATION");

      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      jmsUtil.awaitDlqMessageCount(1, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(0, messageCount, "No messages stored due to validation error");

      int dlqCount = jmsUtil.numberOfDLQMessages();
      assertEquals(1, dlqCount, "Batch with validation error should be in DLQ");

    }

    @Test
    void ensureMessageEndsUpInDlqWithOneInvalidSystemInBatch() throws JsonProcessingException {
      jmsUtil.publishMessage(ActivityType.READ);
      jmsUtil.publishMessage(ActivityType.READ);

      PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ,
          1, ValueInclude.INCLUDE, ValueInclude.INCLUDE);
      pdlLogMessage.setSystemId("invalid");

      jmsUtil.publishMessage(OBJECT_MAPPER.writeValueAsString(pdlLogMessage));

      jmsUtil.publishMessage(ActivityType.READ);
      jmsUtil.publishMessage(ActivityType.READ);

      jmsUtil.awaitDlqMessageCount(1, Duration.ofSeconds(5));
      int dlqCount = jmsUtil.numberOfDLQMessages();

      assertEquals(1, dlqCount, "Batch with validation error should be in DLQ");

    }
  }

  @Nested
  @DisplayName("End-to-End Flow Tests")
  class EndToEndTests {

    @Test
    @DisplayName("Should complete full message flow from JMS to stub")
    void shouldCompleteFullMessageFlow() {
      jmsUtil.publishMessage();

      testabilityUtil.awaitMessageCount(1, Duration.ofSeconds(3));

      var response = testabilityUtil.getAllLogs();
      assertNotNull(response.getBody(), "Should have received logs in stub");
      assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle concurrent message publishing")
    void shouldHandleConcurrentMessages() {
      for (int i = 0; i < 15; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitMessageCount(15, Duration.ofSeconds(5));

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(15, messageCount, "Expected 15 individual log entries");

      int batchCount = testabilityUtil.getBatchCount();
      assertEquals(3, batchCount, "Expected messages to be sent in 3 batches");
    }

    @Test
    @DisplayName("Should not process messages before bulk timeout")
    void shouldNotProcessMessagesBeforeBulkTimeout() {
      for (int i = 0; i < 3; i++) {
        jmsUtil.publishMessage();
      }

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(0, messageCount, "Expected 0 individual log entries");

      int batchCount = testabilityUtil.getBatchCount();
      assertEquals(0, batchCount, "Expected messages to be sent in 0 batches");

      //Fix race condition so the @BeforeEach clears the amq
      testabilityUtil.awaitMessageCount(3, Duration.ofSeconds(5));

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
      for (int i = 0; i < 5; i++) {
        jmsUtil.publishMessage();
      }

      testabilityUtil.awaitBatchCount(1, Duration.ofSeconds(5));
      testabilityUtil.awaitMessageCount(5, Duration.ofSeconds(5));

      testabilityUtil.reset();

      int messageCount = testabilityUtil.getMessageCount();
      assertEquals(0, messageCount, "Stub should be empty after reset");
    }
  }

}
