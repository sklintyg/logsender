/*
 * Copyright (C) 2020 Inera AB (http://www.inera.se)
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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.xml.ws.WebServiceException;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.common.util.integration.json.CustomObjectMapper;
import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.logsender.client.LogSenderClient;
import se.inera.intyg.logsender.converter.LogTypeFactoryImpl;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

/**
 * Created by eriklupander on 2016-03-08.
 */
@ExtendWith(MockitoExtension.class)
public class LogMessageSendProcessorTest {

    @Mock
    private LogSenderClient logSenderClient;

    @Spy
    private LogTypeFactoryImpl logTypeFactory;

    @InjectMocks
    private LogMessageSendProcessor testee;

    private ObjectMapper objectMapper = new CustomObjectMapper();

    @Test
    public void testSendLogMessagesWhenAllOk() throws Exception {
        when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.OK));
        testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
        verify(logSenderClient, times(1)).sendLogMessage(anyList());
    }

    @Test
    public void testSendLogMessagesThrowsPermanentExceptionWhenInvalidJsonIsSupplied() {
        assertThrows(BatchValidationException.class, () -> {
            testee.process(objectMapper.writeValueAsString(buildInvalidGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    public void testSendLogMessagesThrowsBatchValidationExceptionWhenErrorOccured() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.ERROR));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    public void testSendLogMessagesThrowsBatchValidationExceptionWhenValidationErrorOccured() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.VALIDATION_ERROR));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    public void testSendLogMessagesDoesNothingWhenInfoIsReturned() throws Exception {
        when(logSenderClient.sendLogMessage(anyList())).thenReturn(buildResponse(ResultCodeType.INFO));
        testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
        verify(logSenderClient, times(1)).sendLogMessage(anyList());
    }

    @Test
    public void testSendLogMessagesThrowsBatchValidationExceptionWhenIllegalArgumentExceptionIsThrown() {
        assertThrows(BatchValidationException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenThrow(new IllegalArgumentException("illegal"));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    public void testSendLogMessagesThrowsTemporaryExceptionWhenLoggtjanstExecutionExceptionIsThrwn() {
        assertThrows(TemporaryException.class, () -> {
            when(logSenderClient.sendLogMessage(anyList())).thenThrow(new LoggtjanstExecutionException(null));
            testee.process(objectMapper.writeValueAsString(buildGroupedMessages()));
            verify(logSenderClient, times(1)).sendLogMessage(anyList());
        });
    }

    @Test
    public void testSendLogMessagesThrowsTemporaryExceptionWhenWebServiceExceptionIsThrwn() {
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
