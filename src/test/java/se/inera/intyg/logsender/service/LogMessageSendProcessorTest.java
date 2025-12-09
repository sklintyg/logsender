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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.client.LogSenderClient;
import se.inera.intyg.logsender.converter.LogTypeFactoryImpl;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

/**
 * Created by eriklupander on 2016-03-08.
 */
@ExtendWith(MockitoExtension.class)
class LogMessageSendProcessorTest {

    @Mock
    private LogSenderClient logSenderClient;

    @Mock
    MdcHelper mdcHelper;

    @Spy
    private LogTypeFactoryImpl logTypeFactory;

    @InjectMocks
    private LogMessageSendProcessor testee;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(mdcHelper.spanId()).thenReturn("spanId");
        when(mdcHelper.traceId()).thenReturn("traceId");
    }

    @Test
    void testSendLogMessagesWhenAllOk() throws Exception {
        when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.OK));
        testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
        verify(logSenderClient, times(1)).sendLogMessage(anyList());
    }

    @Test
    void testSendLogMessagesThrowsPermanentExceptionWhenInvalidJsonIsSupplied() {
        assertThrows(BatchValidationException.class, () -> {
            testee.process(objectMapper.writeValueAsString(buildInvalidGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    void testSendLogMessagesThrowsBatchValidationExceptionWhenErrorOccured() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.ERROR));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    void testSendLogMessagesThrowsBatchValidationExceptionWhenValidationErrorOccured() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.VALIDATION_ERROR));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    void testSendLogMessagesDoesNothingWhenInfoIsReturned() throws Exception {
        when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.INFO));
        testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
        verify(logSenderClient, times(1)).sendLogMessage(anyList());
    }

    @Test
    void testSendLogMessagesThrowsBatchValidationExceptionWhenIllegalArgumentExceptionIsThrown() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenThrow(new IllegalArgumentException("illegal"));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    void testSendLogMessagesThrowsTemporaryExceptionWhenLoggtjanstExecutionExceptionIsThrwn() {
        assertThrows(TemporaryException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenThrow(new LoggtjanstExecutionException(null));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    void testSendLogMessagesThrowsTemporaryExceptionWhenWebServiceExceptionIsThrwn() {
        assertThrows(TemporaryException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenThrow(new WebServiceException());
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }


    private StoreLogResponseType buildResponse(ResultCodeType resultCodeType) {
        StoreLogResponseType responseType = new StoreLogResponseType();
        ResultType resultType = new ResultType();
        resultType.setResultCode(resultCodeType);
        responseType.setResult(resultType);
        return responseType;
    }

    private List<String> buildGroupedMessages() {
        String pdlLogMessage1 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ);
        String pdlLogMessage2 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.PRINT);
        return Arrays.asList(pdlLogMessage1, pdlLogMessage2);
    }

    private List<String> buildInvalidGroupedMessages() {
        String pdlLogMessage1 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ);
        String pdlLogMessage2 = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.PRINT);
        return Arrays.asList(pdlLogMessage1, pdlLogMessage2, "this-is-not-json");
    }
}
