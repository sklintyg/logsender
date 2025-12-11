package se.inera.intyg.logsender.integrationtest.util;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import se.inera.intyg.certificateanalyticsservice.application.messages.model.PseudonymizedAnalyticsMessage;

public class TestabilityUtil {

  private final int port;
  private final TestRestTemplate restTemplate;

  public TestabilityUtil(TestRestTemplate restTemplate, int port) {
    this.restTemplate = restTemplate;
    this.port = port;
  }

  public PseudonymizedAnalyticsMessage awaitProcessed(String messageId, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .until(() -> {
              final var resp = restTemplate.getForEntity(
                  "http://localhost:%s/testability/messages/v1/%s".formatted(port, messageId),
                  PseudonymizedAnalyticsMessage.class
              );
              return resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null;
            }
        );

    return restTemplate.getForEntity(
        "http://localhost:%s/testability/messages/v1/%s".formatted(port, messageId),
        PseudonymizedAnalyticsMessage.class
    ).getBody();
  }

  public void reset() {
    restTemplate.getForEntity(
        "http://localhost:%s/testability/messages/reset".formatted(port),
        Void.class
    );
  }

  public void toggleTemporaryFailure(int numberOfFailures) {
    restTemplate.getForEntity(
        "http://localhost:%s/testability/messages/fail/temporary/%s"
            .formatted(port, numberOfFailures),
        Void.class
    );
  }

  public void togglePermanentFailure(boolean permanentFailure) {
    restTemplate.getForEntity(
        "http://localhost:%s/testability/messages/fail/permanent/%s"
            .formatted(port, permanentFailure),
        Void.class
    );
  }
}