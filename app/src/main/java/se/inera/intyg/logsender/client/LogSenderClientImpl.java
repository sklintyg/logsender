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
package se.inera.intyg.logsender.client;

import jakarta.xml.ws.WebServiceException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.service.SoapIntegrationService;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogSenderClientImpl implements LogSenderClient {

  private final LogsenderProperties properties;
  private final SoapIntegrationService soapIntegrationService;

  @Override
  public StoreLogResponseType sendLogMessage(List<LogType> logEntries) {

    if (logEntries == null || logEntries.isEmpty()) {
      final var response = new StoreLogResponseType();
      final var resultType = new ResultType();
      resultType.setResultCode(ResultCodeType.INFO);
      resultType.setResultText("No log entries supplied, not invoking storeLog");
      response.setResult(resultType);
      return response;
    }

    final var request = new StoreLogType();
    request.getLog().addAll(logEntries);

    try {
      final var response = soapIntegrationService.storeLog(
          properties.getLoggtjanst().getLogicalAddress(),
          request
      );
      if (response.getResult().getResultCode() == ResultCodeType.OK && (log.isDebugEnabled())) {
        log.debug("Successfully sent {} PDL log entries for ID's: {}", logEntries.size(),
            logEntries.stream()
                .map(LogType::getLogId)
                .collect(Collectors.joining(", ")));
      }
      return response;
    } catch (WebServiceException e) {
      throw new LoggtjanstExecutionException(e);
    }
  }
}
