package se.inera.intyg.logsender.integrationtest.util;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Enumeration;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.integrationtest.helper.TestDataHelper;
import se.inera.intyg.logsender.model.ActivityType;

public class JmsUtil {

  public final String dlqQueueName;

  private final JmsTemplate jmsTemplate;
  private final String queueName;

  public JmsUtil(JmsTemplate jmsTemplate, LogsenderProperties properties) {
    this.jmsTemplate = jmsTemplate;
    this.queueName = properties.getQueue().getReceiveLogMessageEndpoint()
        .replace("activemq:queue:", "");
    this.dlqQueueName = properties.getQueue().getReceiveAggregatedLogMessageDlq()
        .replace("activemq:queue:", "");
  }

  public void reset() {
    purgeQueue(queueName);
    purgeQueue(dlqQueueName);
  }

  public void publishMessage() {
    jmsTemplate.convertAndSend(queueName,
        TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ));
  }

  public void publishMessage(String message) {
    jmsTemplate.convertAndSend(queueName, message);
  }

  public void publishMessage(ActivityType activityType) {
    jmsTemplate.convertAndSend(queueName,
        TestDataHelper.buildBasePdlLogMessageAsJson(activityType));
  }


  public void awaitDlqMessageCount(int expectedCount, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> {
          int actualCount = numberOfDLQMessages();
          if (actualCount < expectedCount) {
            throw new AssertionError(
                String.format(
                    "Expected %d messages in DLQ, but found %d after waiting %s.",
                    expectedCount, actualCount, timeout
                )
            );
          }
        });
  }

  private void purgeQueue(String queueName) {
    jmsTemplate.setReceiveTimeout(100);
    while (true) {
      final var msg = jmsTemplate.receive(queueName);
      if (msg == null) {
        break;
      }
    }
  }


  public Integer numberOfDLQMessages() {
    return (Integer) jmsTemplate.browse(dlqQueueName,
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
}