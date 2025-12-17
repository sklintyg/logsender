/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
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

import jakarta.xml.ws.WebServiceException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

@Slf4j
@RequiredArgsConstructor
public class StoreLogStubResponder implements StoreLogResponderInterface {

  private final LogStore logStore;
  private final StubState stubState;

  @Override
  public StoreLogResponseType storeLog(String logicalAddress, StoreLogType request) {
    log.info("StoreLogStubResponder.storeLog called with {} log entries",
        request != null && request.getLog() != null ? request.getLog().size() : 0);
    final var response = new StoreLogResponseType();
    final var result = new ResultType();

    if (stubState != null) {

      if (stubState.getArtificialLatency() > 0L) {
        try {
          Thread.sleep(stubState.getArtificialLatency());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // Restore interrupt status
          log.warn("Sleep interrupted while applying artificial latency", e);
        }
      }

      if (!stubState.isActive()) {
        throw new WebServiceException("Stub is faking unaccessible StoreLog service");
      } else if (stubState.isActive() && stubState.isFakeError()) {
        result.setResultCode(ResultCodeType.ERROR);
        result.setResultText("Stub is faking errors.");
        response.setResult(result);
        return response;
      }

      if (stubState.getErrorState() != null && stubState.getErrorState() != ErrorState.NONE) {
        switch (stubState.getErrorState()) {
          case ERROR:
            result.setResultCode(ResultCodeType.ERROR);
            break;
          case VALIDATION:
            result.setResultCode(ResultCodeType.VALIDATION_ERROR);
            break;
          default:
            result.setResultCode(ResultCodeType.OK);
            break;
        }
        response.setResult(result);
        result.setResultText("Stub is triggering error: " + stubState.getErrorState().name());
        return response;
      }
    }

    assert request != null;
    List<LogType> logItems = request.getLog();

    boolean hasInvalid = logItems.stream()
        .anyMatch(item -> Objects.equals(item.getSystem().getSystemId(), "invalid"));

    if (hasInvalid) {
      log.info("Storelog called with artificial \"invalid\" log entries");
      result.setResultCode(ResultCodeType.VALIDATION_ERROR);
      result.setResultText("Invalid log ID");
      response.setResult(result);
      return response;
    }

    log.info("Storing {} log items to LogStore", logItems.size());

    if (stubState != null) {
      stubState.incrementBatchCount();
    }

    for (LogType lt : logItems) {
      logStore.addLogItem(lt);
      log.debug("Stored log item with ID: {}", lt.getLogId());
    }

    result.setResultCode(ResultCodeType.OK);
    result.setResultText("Done");
    response.setResult(result);
    log.info("Successfully stored {} log entries", logItems.size());
    return response;
  }


}
