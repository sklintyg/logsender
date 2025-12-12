package se.inera.intyg.logsender.integrationtest.util;

import static org.awaitility.Awaitility.await;

import jakarta.jms.Message;
import java.time.Duration;
import java.util.function.Predicate;
import org.springframework.jms.core.JmsTemplate;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.model.ActivityType;

public class JmsUtil {

  public static final String DLQ_QUEUE_NAME = "ActiveMQ.DLQ";

  private final JmsTemplate jmsTemplate;
  private final String queueName;

  public JmsUtil(JmsTemplate jmsTemplate, String queueName) {
    this.jmsTemplate = jmsTemplate;
    this.queueName = queueName;
  }

  public void reset() {
    purgeQueue(queueName);
    purgeQueue(DLQ_QUEUE_NAME);
  }

  public void publishMessage() {
    jmsTemplate.convertAndSend(queueName,
        TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ));
  }

  public boolean awaitProcessedToDlq(String messageId, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .until(() -> dlqContains(messageId));
    return true;
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

  private boolean dlqContains(String messageId) {
    final var pred = matchingMessageIdPredicate(messageId);
    return Boolean.TRUE.equals(
        jmsTemplate.browse(DLQ_QUEUE_NAME, (session, browser) -> {
              var e = browser.getEnumeration();
              while (e.hasMoreElements()) {
                final var m = (Message) e.nextElement();
                if (pred.test(m)) {
                  return true;
                }
              }
              return false;
            }
        )
    );
  }

  private Predicate<Message> matchingMessageIdPredicate(String messageId) {
    return m -> {
      try {
        return messageId.equals(m.getStringProperty("messageId"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}