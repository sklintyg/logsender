package se.inera.intyg.logsender.integrationtest.util;

import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import se.riv.informationsecurity.auditing.log.v2.LogType;

public class TestabilityUtil {

  private final int port;
  private final TestRestTemplate restTemplate;

  public TestabilityUtil(TestRestTemplate restTemplate, int port) {
    this.restTemplate = restTemplate;
    this.port = port;
  }

  /**
   * Wait for a specific log message to be processed by checking the stub store.
   */
  public LogType awaitProcessed(String logId, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .until(() -> {
              final var resp = getLogById(logId);
              return resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null;
            }
        );
    return getLogById(logId).getBody();
  }

  /**
   * Wait for the stub to have received a specific number of messages.
   */
  public void awaitMessageCount(int expectedCount, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .until(() -> getMessageCount() >= expectedCount);
  }

  /**
   * Get a specific log message from the stub by log ID.
   */
  public ResponseEntity<LogType> getLogById(String logId) {
    return restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/logs/%s".formatted(port, logId),
        LogType.class
    );
  }

  /**
   * Get all logs stored in the stub.
   */
  public ResponseEntity<LogType[]> getAllLogs() {
    return restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/logs".formatted(port),
        LogType[].class
    );
  }

  /**
   * Get the count of messages received by the stub.
   */
  public int getMessageCount() {
    final var resp = getAllLogs();
    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
      return resp.getBody().length;
    }
    return 0;
  }

  /**
   * Reset the stub - clear all stored messages.
   */
  public void reset() {
    restTemplate.delete(
        "http://localhost:%s/api/loggtjanst-api/logs".formatted(port)
    );
  }

  /**
   * Get stub state (for debugging).
   */
  public ResponseEntity<JsonNode> getState() {
    return restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/state".formatted(port),
        JsonNode.class
    );
  }
}