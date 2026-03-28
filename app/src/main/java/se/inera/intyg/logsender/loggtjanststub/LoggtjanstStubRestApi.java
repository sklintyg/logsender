/*
 * Copyright (C) 2026 Inera AB (http://www.inera.se)
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
package se.inera.intyg.logsender.loggtjanststub;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.riv.informationsecurity.auditing.log.v2.LogType;

@RestController
@RequestMapping("/api/loggtjanst-api")
@Profile({"testability"})
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
      return ResponseEntity.internalServerError()
          .body(
              "{\"status\":\"ERROR\",\"message\":\"Unknown ErrorState: "
                  + errorType
                  + ". Allowed values are NONE, ERROR, VALIDATION\"}");
    }
  }

  @GetMapping("/latency/{latencyMillis}")
  public ResponseEntity<String> setLatency(@PathVariable Long latencyMillis) {
    stubState.setArtificialLatency(latencyMillis);
    return ResponseEntity.ok("OK");
  }
}
