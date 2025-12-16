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
package se.inera.intyg.logsender.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.ws.WebServiceException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.inera.intyg.logsender.client.LogSenderClient;
import se.inera.intyg.logsender.converter.LogTypeFactory;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.logging.MdcCloseableMap;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.logging.MdcLogConstants;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

@Component
@RequiredArgsConstructor
public class LogMessageSendProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(LogMessageSendProcessor.class);

  private final LogSenderClient logSenderClient;

  private final LogTypeFactory logTypeFactory;

  private final ObjectMapper objectMapper;

  private final MdcHelper mdcHelper;

  public void process(String groupedLogEntries)
      throws IOException, BatchValidationException, TemporaryException {

    try (MdcCloseableMap mdc = MdcCloseableMap.builder()
        .put(MdcLogConstants.TRACE_ID_KEY, mdcHelper.traceId())
        .put(MdcLogConstants.SPAN_ID_KEY, mdcHelper.spanId())
        .build()
    ) {
      List<String> groupedList = objectMapper.readValue(groupedLogEntries, List.class);

      List<LogType> logMessages = groupedList.stream()
          .map(this::jsonToPdlLogMessage)
          .map(logTypeFactory::convert)
          .toList();

      StoreLogResponseType response = logSenderClient.sendLogMessage(logMessages);

      final ResultType result = response.getResult();
      final String resultText = result.getResultText();
      final var resultCodeValue = result.getResultCode().value();

      switch (result.getResultCode()) {
        case OK:
          break;
        case ERROR, VALIDATION_ERROR:
          LOG.error(
              "Loggtjänsten rejected PDL message batch with {}, batch will be moved to DLQ. Result text: '{}'",
              resultCodeValue, resultText);
          throw new BatchValidationException(
              "Loggtjänsten rejected PDL message batch with error: " + resultText
                  + ". Batch will be moved directly to DLQ.");
        case INFO:
          LOG.warn(
              "Warning of type INFO occured when sending PDL log message batch: '{}'. Will not requeue.",
              resultText);
          break;
        default:
          throw new TemporaryException(resultText);
      }

    } catch (IllegalArgumentException e) {
      LOG.error("Moving batch to DLQ.");
      throw new BatchValidationException("Unparsable Log message: " + e);

    } catch (LoggtjanstExecutionException e) {
      LOG.warn("Call to send log message caused a LoggtjanstExecutionException. Will retry.");
      throw new TemporaryException(e);

    } catch (WebServiceException e) {
      LOG.warn("Call to send log message caused an error. Will retry.");
      throw new TemporaryException(e);
    }
  }

  private PdlLogMessage jsonToPdlLogMessage(String body) {
    try {
      return objectMapper.readValue(body, PdlLogMessage.class);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not parse PdlLogMessage from log message JSON: " + e);
    }
  }
}
