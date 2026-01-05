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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.ws.WebServiceException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.logsender.client.LogSenderClient;
import se.inera.intyg.logsender.converter.LogTypeFactoryImpl;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.ActivityType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

@ExtendWith(MockitoExtension.class)
class LogMessageSendProcessorTest {

  private static final ObjectMapper objectMapper = new CustomObjectMapper();

  @Mock
  private LogSenderClient logSenderClient;

  @Spy
  private LogTypeFactoryImpl logTypeFactory;

  private LogMessageSendProcessor logMessageSendProcessor;

  @BeforeEach
  void setUp() {
    logMessageSendProcessor = new LogMessageSendProcessor(logSenderClient, logTypeFactory,
        objectMapper);
  }

  @Test
  void testSendLogMessagesWhenAllOk() throws Exception {
    when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.OK));
    logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    verify(logSenderClient, times(1)).sendLogMessage(anyList());
  }

  @Test
  void testSendLogMessagesThrowsPermanentExceptionWhenInvalidJsonIsSupplied() {
    assertThrows(BatchValidationException.class, () -> {
      logMessageSendProcessor.process(
          objectMapper.writeValueAsString(buildInvalidGroupedMessages()));
    });
  }

  @Test
  void testSendLogMessagesThrowsBatchValidationExceptionWhenErrorOccured() {
    when(logSenderClient.sendLogMessage(anyList())).thenReturn(
        buildResponse(ResultCodeType.ERROR));

    assertThrows(BatchValidationException.class, () -> {
      logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    });
  }

  @Test
  void testSendLogMessagesThrowsBatchValidationExceptionWhenValidationErrorOccured() {
    when(logSenderClient.sendLogMessage(anyList())).thenReturn(
        buildResponse(ResultCodeType.VALIDATION_ERROR));

    assertThrows(BatchValidationException.class, () -> {
      logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    });
  }

  @Test
  void testSendLogMessagesDoesNothingWhenInfoIsReturned() throws Exception {
    when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.INFO));
    logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    verify(logSenderClient, times(1)).sendLogMessage(anyList());
  }

  @Test
  void testSendLogMessagesThrowsBatchValidationExceptionWhenIllegalArgumentExceptionIsThrown() {
    when(logSenderClient.sendLogMessage(anyList())).thenThrow(
        new IllegalArgumentException("illegal"));

    assertThrows(BatchValidationException.class, () -> {
      logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    });

    verify(logSenderClient, times(1)).sendLogMessage(anyList());
  }

  @Test
  void testSendLogMessagesThrowsTemporaryExceptionWhenLoggtjanstExecutionExceptionIsThrwn() {
    when(logSenderClient.sendLogMessage(anyList())).thenThrow(
        new LoggtjanstExecutionException(null));

    assertThrows(TemporaryException.class, () -> {
      logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    });

    verify(logSenderClient, times(1)).sendLogMessage(anyList());
  }

  @Test
  void testSendLogMessagesThrowsTemporaryExceptionWhenWebServiceExceptionIsThrwn() {
    when(logSenderClient.sendLogMessage(anyList())).thenThrow(new WebServiceException());

    assertThrows(TemporaryException.class, () -> {
      logMessageSendProcessor.process(objectMapper.writeValueAsString(buildGroupedMessages()));
    });

    verify(logSenderClient, times(1)).sendLogMessage(anyList());
  }

  private StoreLogResponseType buildResponse(ResultCodeType resultCodeType) {
    final var responseType = new StoreLogResponseType();
    final var resultType = new ResultType();
    resultType.setResultCode(resultCodeType);
    responseType.setResult(resultType);
    return responseType;
  }

  private List<String> buildGroupedMessages() {
    final var pdlLogMessage1 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ);
    final var pdlLogMessage2 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.PRINT);
    return Arrays.asList(pdlLogMessage1, pdlLogMessage2);
  }

  private List<String> buildInvalidGroupedMessages() {
    final var pdlLogMessage1 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ);
    final var pdlLogMessage2 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.PRINT);
    return Arrays.asList(pdlLogMessage1, pdlLogMessage2, "this-is-not-json");
  }
}
