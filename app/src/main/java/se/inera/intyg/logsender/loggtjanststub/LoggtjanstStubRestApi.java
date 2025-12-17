package se.inera.intyg.logsender.loggtjanststub;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import se.riv.informationsecurity.auditing.log.v2.LogType;

@RestController
@RequestMapping("/api/loggtjanst-api")
@Profile({"dev", "testability-api"})
@RequiredArgsConstructor
public class LoggtjanstStubRestApi {

  private final LogStore logStore;
  private final StubState stubState;

  @GetMapping
  public Collection<LogType> getAllLogEntries() {
    return logStore.getAll();
  }

  @DeleteMapping("/logs")
  public ResponseEntity<Void> deleteLogStore() {
    logStore.clear();
    stubState.resetBatchCount();
    return ResponseEntity.ok().build();
  }

  @GetMapping("/batch-count")
  public ResponseEntity<String> getBatchCount() {
    return ResponseEntity.ok("{\"batchCount\":" + stubState.getBatchCount() + "}");
  }

  @GetMapping("/online")
  public ResponseEntity<String> activateStub() {
    stubState.setActive(true);
    return ResponseEntity.ok("{\"status\":\"OK\",\"active\":true}");
  }

  @GetMapping("/offline")
  public ResponseEntity<String> deactivateStub() {
    stubState.setActive(false);
    return ResponseEntity.ok("{\"status\":\"OK\",\"active\":false}");
  }

  @GetMapping("/logs")
  public LogType[] getAllLogs() {
    final var logs = logStore.getAll();
    return logs.toArray(new LogType[0]);
  }

  @GetMapping("/error/{errorType}")
  public ResponseEntity<String> activateErrorState(@PathVariable String errorType) {
    try {
      final var errorState = ErrorState.valueOf(errorType);
      stubState.setErrorState(errorState);
      return ResponseEntity.ok("{\"status\":\"OK\",\"errorState\":\"" + errorType + "\"}");
    } catch (IllegalArgumentException e) {
      return ResponseEntity
          .internalServerError()
          .body("{\"status\":\"ERROR\",\"message\":\"Unknown ErrorState: " + errorType
              + ". Allowed values are NONE, ERROR, VALIDATION\"}");
    }
  }

  @GetMapping("/latency/{latencyMillis}")
  public ResponseEntity<String> setLatency(@PathVariable Long latencyMillis) {
    stubState.setArtificialLatency(latencyMillis);
    return ResponseEntity.ok("OK");
  }
}
