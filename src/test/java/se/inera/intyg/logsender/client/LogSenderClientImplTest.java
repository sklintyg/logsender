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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.exception.LoggtjanstExecutionException;
import se.inera.intyg.logsender.service.SoapIntegrationService;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

/**
 * Created by eriklupander on 2016-03-08.
 */

@ExtendWith(MockitoExtension.class)
class LogSenderClientImplTest {

    @Mock
    private LogsenderProperties properties;

    @Mock
    private LogsenderProperties.Loggtjanst loggtjanst;

    @Mock
    private SoapIntegrationService soapIntegrationService;

    private LogSenderClientImpl testee;

    @BeforeEach
    void setUp() {

        testee = new LogSenderClientImpl(properties, soapIntegrationService);
    }

    @Test
    void testSendOk() {
        when(properties.getLoggtjanst()).thenReturn(loggtjanst);
        when(loggtjanst.getLogicalAddress()).thenReturn("TEST-LOGICAL-ADDRESS");
        when(soapIntegrationService.storeLog(any(), any(StoreLogType.class))).thenReturn(buildOkResponse());

        StoreLogResponseType response = testee.sendLogMessage(buildLogEntries());
        assertNotNull(response);
        assertEquals(ResultCodeType.OK, response.getResult().getResultCode());
    }

    @Test
    void testSendError() {
        when(properties.getLoggtjanst()).thenReturn(loggtjanst);
        when(loggtjanst.getLogicalAddress()).thenReturn("TEST-LOGICAL-ADDRESS");
        when(soapIntegrationService.storeLog(any(), any(StoreLogType.class))).thenReturn(buildErrorResponse());

        StoreLogResponseType response = testee.sendLogMessage(buildLogEntries());
        assertNotNull(response);
        assertEquals(ResultCodeType.ERROR, response.getResult().getResultCode());
    }

    @Test
    void testSendWithNullListCausesNoSend() {
        StoreLogResponseType response = testee.sendLogMessage(null);
        assertNotNull(response);
        assertEquals(ResultCodeType.INFO, response.getResult().getResultCode());
        assertNotNull(response.getResult().getResultText());
        verify(soapIntegrationService, times(0)).storeLog(anyString(), any(StoreLogType.class));
    }

    @Test
    void testSendWithEmptyLogEntriesListCausesNoSend() {
        StoreLogResponseType response = testee.sendLogMessage(new ArrayList<>());
        assertNotNull(response);
        assertEquals(ResultCodeType.INFO, response.getResult().getResultCode());
        assertNotNull(response.getResult().getResultText());
        verify(soapIntegrationService, times(0)).storeLog(anyString(), any(StoreLogType.class));
    }

    @Test
    void testWebServiceExceptionCausesLoggtjanstExecutionException() {
        when(properties.getLoggtjanst()).thenReturn(loggtjanst);
        when(loggtjanst.getLogicalAddress()).thenReturn("TEST-LOGICAL-ADDRESS");

        assertThrows(LoggtjanstExecutionException.class, () -> {
            when(soapIntegrationService.storeLog(any(), any(StoreLogType.class))).thenThrow(new WebServiceException("error"));
            testee.sendLogMessage(buildLogEntries());
        });
    }

    private StoreLogResponseType buildOkResponse() {
        StoreLogResponseType resp = new StoreLogResponseType();
        ResultType resultType = new ResultType();
        resultType.setResultCode(ResultCodeType.OK);
        resp.setResult(resultType);
        return resp;
    }

    private StoreLogResponseType buildErrorResponse() {
        StoreLogResponseType resp = new StoreLogResponseType();
        ResultType resultType = new ResultType();
        resultType.setResultCode(ResultCodeType.ERROR);
        resp.setResult(resultType);
        return resp;
    }

    private List<LogType> buildLogEntries() {
        List<LogType> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry());
        return logEntries;
    }

    private LogType buildLogEntry() {
        return new LogType();
    }
}
