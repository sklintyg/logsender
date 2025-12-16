/*
 * Copyright (C) 2024 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.intyg.logsender.integrationtest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.helper.ValueInclude;
import se.inera.intyg.logsender.integrationtest.util.Containers;
import se.inera.intyg.logsender.integrationtest.util.IntegrationTestConfig;
import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.PdlLogMessage;

@Slf4j
@CamelSpringBootTest
@SpringBootTest
@Import(IntegrationTestConfig.class)
@ActiveProfiles({"dev", "testability-api", "integration-test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RouteIT {

  // Initialize containers BEFORE Spring context loads
  static {
    Containers.ensureRunning();
  }

  @Autowired
  private JmsTemplate jmsTemplate;
  @Autowired
  @Qualifier("newLogMessageQueue")
  private Queue sendQueue;
  @Autowired
  @Qualifier("newAggregatedLogMessageDLQ")
  private Queue newAggregatedLogMessageDLQ;
  @Autowired
  private MockLogSenderClientImpl mockLogSenderClient;

  @BeforeEach
  void resetStub() {
    mockLogSenderClient.reset();

    // Simple DLQ clear - with @DirtiesContext and persistent=false,
    // this should be sufficient
    clearQueue(newAggregatedLogMessageDLQ);
  }

  private void clearQueue(Queue queue) {
    Long originalTimeout = jmsTemplate.getReceiveTimeout();
    jmsTemplate.setReceiveTimeout(100L);

    // Consume all messages from the queue
    int cleared = 0;
    while (jmsTemplate.receiveAndConvert(queue) != null) {
      cleared++;
    }

    if (cleared > 0) {
      log.debug("Cleared {} messages from DLQ", cleared);
    }

    jmsTemplate.setReceiveTimeout(originalTimeout);
  }

  @Test
  void ensureStubReceivesOneMessageAfterSixHasBeenSent() {
    for (int a = 0; a < 6; a++) {
      sendMessage(ActivityType.READ);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(1));
  }

  @Test
  void ensureStubReceivesTwoMessagesAfterTenHasBeenSent() {
    for (int a = 0; a < 10; a++) {
      sendMessage(ActivityType.READ);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(2));
  }

  @Test
  void ensureStubReceivesZeroMessagesAfterThreeHasBeenSent() {
    for (int a = 0; a < 3; a++) {
      sendMessage(ActivityType.READ);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(0));
  }

  @Test
  void ensureStubReceivesOneMessagesAfterOneWithFiveResourcesHasBeenSent() {
    for (int a = 0; a < 1; a++) {
      sendMessage(ActivityType.READ, 5);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(1));
  }

  @Test
  void ensureStubReceivesSixMessagesAfterThreeTimesTenHasBeenSent() {
    for (int a = 0; a < 3; a++) {
      sendMessage(ActivityType.READ, 10);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(6));
  }

  @Test
  void ensureStubReceivesZeroMessagesAfterFiveWithNoResourcesHasBeenSent() {
    for (int a = 0; a < 5; a++) {
      sendMessage(ActivityType.READ, 0);
    }
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(0));
  }

  @Test
  void ensureStubReceivesNoMessageAfterOneWithSixWhereOnIsInvalidHasBeenSent()
      throws IOException {
    String body = buildPdlLogMessageWithInvalidResourceJson();
    jmsTemplate.send(sendQueue, session -> {
      try {
        return session.createTextMessage(body);
      } catch (JMSException e) {
        throw new RuntimeException(e);
      }
    });
    await().atMost(5, TimeUnit.SECONDS).until(() -> messagesReceived(0));
  }

  @Test
  void ensureMessageEndsUpInDLQ() {
    // Send 5 EMERGENCY_ACCESS messages (will form 1 batch that fails)
    for (int a = 0; a < 5; a++) {
      sendMessage(ActivityType.EMERGENCY_ACCESS);
    }

    // Wait patiently for message to go through: aggregate -> fail -> redelivery -> fail -> DLQ
    await()
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          int actualDLQMessages = numberOfDLQMessages();
          log.info("DLQ message count: {}", actualDLQMessages);
          assertThat(actualDLQMessages)
              .withFailMessage(
                  "Expected 1 message in DLQ after sending 5 EMERGENCY_ACCESS messages, but found %d",
                  actualDLQMessages)
              .isEqualTo(1);
        });
  }

  @Test
  void ensureTwoMessagesEndsUpInDLQ() {
    // Send 10 EMERGENCY_ACCESS messages
    // They will form batches that fail and go to DLQ
    for (int a = 0; a < 10; a++) {
      sendMessage(ActivityType.EMERGENCY_ACCESS);
    }

    // Wait patiently for both batches to reach DLQ
    // No need to control timing - just wait for the final state
    await()
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          int actualDLQMessages = numberOfDLQMessages();
          log.info("DLQ message count: {}", actualDLQMessages);
          assertThat(actualDLQMessages)
              .withFailMessage(
                  "Expected 2 messages in DLQ after sending 10 EMERGENCY_ACCESS messages, but found %d",
                  actualDLQMessages)
              .isEqualTo(2);
        });
  }

  @Test
  void ensureMessageEndsUpInDlqWithOneInvalidSystemInBatch() {
    sendMessage(ActivityType.READ, 2);

    jmsTemplate.send(sendQueue, session -> {
      try {
        PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ,
            1, ValueInclude.INCLUDE, ValueInclude.INCLUDE);
        pdlLogMessage.setSystemId("invalid");
        return session.createTextMessage(new CustomObjectMapper().registerModule(new JavaTimeModule())
            .writeValueAsString(pdlLogMessage));
      } catch (JMSException | JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });

    sendMessage(ActivityType.READ, 2);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          int actualDLQMessages = numberOfDLQMessages();
          assertThat(actualDLQMessages)
              .withFailMessage(
                  "Expected 1 message in DLQ (the invalid one), but found %d",
                  actualDLQMessages)
              .isEqualTo(1);
        });
  }

  @Test
  void ensureStubReceivesOneMessageDueToTimeoutWhenTwoMessagesHaveBeenSent() {
    for (int a = 0; a < 2; a++) {
      sendMessage(ActivityType.READ);
    }

    await().atMost(2, TimeUnit.SECONDS).until(() -> messagesReceived(1));
  }

  @Test
  void ensureStubReceivesZeroMessagesDueToTimeoutNotExpiredWhenTwoMessagesHaveBeenSent() {

    for (int a = 0; a < 2; a++) {
      sendMessage(ActivityType.READ);
    }

    await().atMost(500, TimeUnit.MILLISECONDS).until(() -> messagesReceived(0));
  }


  private void sendMessage(final ActivityType activityType, int numberOfResources) {
    jmsTemplate.send(sendQueue, session -> {
      try {
        return session.createTextMessage(
            TestDataHelper.buildBasePdlLogMessageAsJson(activityType, numberOfResources));
      } catch (JMSException e) {
        throw new RuntimeException(e);
      }
    });

    // Small delay to ensure predictable aggregation timing
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void sendMessage(final ActivityType activityType) {
    sendMessage(activityType, 1);
  }

  private Integer numberOfDLQMessages() {
    return (Integer) jmsTemplate.browse(newAggregatedLogMessageDLQ,
        (BrowserCallback<Object>) (session, browser) -> {
          int counter = 0;
          Enumeration<?> msgs = browser.getEnumeration();
          while (msgs.hasMoreElements()) {
            msgs.nextElement();
            counter++;
          }
          return counter;
        });
  }

  private Boolean messagesReceived(int expected) {
    int numberOfReceivedMessages = mockLogSenderClient.getNumberOfReceivedMessages();
    log.info("numberOfReceivedMessages: {}", numberOfReceivedMessages);
    return (numberOfReceivedMessages == expected);
  }


  private String buildPdlLogMessageWithInvalidResourceJson() throws IOException {
    String bodyOfSix = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ, 6);
    ObjectNode jsonNode = (ObjectNode) new CustomObjectMapper().readTree(bodyOfSix);
    ArrayNode pdlResourceList = (ArrayNode) jsonNode.get("pdlResourceList");

    JsonNode invalidJsonNode = new TextNode("Some text that doesn't belong here");
    ObjectNode resourceNode = (ObjectNode) pdlResourceList.get(2);
    resourceNode.set("resourceOwner", invalidJsonNode);

    return new CustomObjectMapper().writeValueAsString(jsonNode);
  }
}
