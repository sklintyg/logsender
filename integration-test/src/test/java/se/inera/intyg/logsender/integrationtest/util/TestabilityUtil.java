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

  public TestabilityUtil(int port) {
    this.restTemplate = new TestRestTemplate();
    this.port = port;
  }

  /**
   * Wait for the stub to have received a specific number of messages.
   */
  public void awaitMessageCount(int expectedCount, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .failFast(() -> {
          return false; // Never fail fast, just used for logging
        })
        .untilAsserted(() -> {
          final var actualCount = getMessageCount();
          if (actualCount < expectedCount) {
            final var logs = getAllLogs();
            String logDetails = "";
            if (logs.getBody() != null && logs.getBody().length > 0) {
              logDetails = "\nReceived log IDs: " +
                  java.util.Arrays.stream(logs.getBody())
                      .map(LogType::getLogId)
                      .collect(java.util.stream.Collectors.joining(", "));
            }
            throw new AssertionError(
                String.format(
                    "Expected %d messages in stub, but found %d after waiting %s.%s",
                    expectedCount, actualCount, timeout, logDetails
                )
            );
          }
        });
  }

  /**
   * Wait for the stub to have received a specific number of batches.
   */
  public void awaitBatchCount(int expectedCount, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> {
          final var actualCount = getBatchCount();
          if (actualCount < expectedCount) {
            throw new AssertionError(
                String.format(
                    "Expected %d batches in stub, but found %d after waiting %s.",
                    expectedCount, actualCount, timeout
                )
            );
          }
        });
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
    setStubOnline();
    setErrorState("NONE");
  }

  /**
   * Get the batch count (number of times storeLog was called).
   */
  public int getBatchCount() {
    final var resp = restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/batch-count".formatted(port),
        JsonNode.class
    );
    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
      return resp.getBody().get("batchCount").asInt();
    }
    return 0;
  }

  /**
   * Set stub error state (NONE, ERROR, VALIDATION).
   */
  public void setErrorState(String errorType) {
    restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/error/%s".formatted(port, errorType),
        JsonNode.class
    );
  }

  /**
   * Set stub offline (will reject all requests).
   */
  public void setStubOffline() {
    restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/offline".formatted(port),
        JsonNode.class
    );
  }

  /**
   * Set stub online (will accept requests).
   */
  public void setStubOnline() {
    restTemplate.getForEntity(
        "http://localhost:%s/api/loggtjanst-api/online".formatted(port),
        JsonNode.class
    );
  }
}