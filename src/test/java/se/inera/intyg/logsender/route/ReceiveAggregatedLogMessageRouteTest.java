/*
 * Copyright (C) 2022 Inera AB (http://www.inera.se)
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
package se.inera.intyg.logsender.route;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import javax.xml.ws.WebServiceException;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.google.common.collect.ImmutableMap;

import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.testconfig.UnitTestConfig;

@CamelSpringTest
@ContextConfiguration(classes = UnitTestConfig.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:logsender/unit-test.properties")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class ReceiveAggregatedLogMessageRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Produce("direct://receiveAggregatedLogMessageEndpoint")
    protected ProducerTemplate producerTemplate;

    @EndpointInject("mock:bean:logMessageSendProcessor")
    protected MockEndpoint logMessageSendProcessor;

    @EndpointInject("mock:direct:logMessagePermanentErrorHandlerEndpoint")
    protected MockEndpoint logMessagePermanentErrorHandlerEndpoint;

    @EndpointInject("mock:direct:logMessageTemporaryErrorHandlerEndpoint")
    protected MockEndpoint logMessageTemporaryErrorHandlerEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        MockEndpoint.resetMocks(camelContext);

        AdviceWithRouteBuilder.adviceWith(camelContext, "aggregatedJmsToSenderRoute", in ->
                in.mockEndpointsAndSkip("direct:logMessageTemporaryErrorHandlerEndpoint",
                    "bean:logMessageSendProcessor", "direct:logMessagePermanentErrorHandlerEndpoint"));
    }

    @Test
    @DirtiesContext
    public void testNormalLogStoreRoute() throws InterruptedException {
        // Given
        logMessageSendProcessor.expectedMessageCount(1);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        // When
        for (int a = 0; a < 1; a++) {
            producerTemplate.sendBodyAndHeaders(Collections.singletonList(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)),
                ImmutableMap.of());
        }
        // Then
        assertIsSatisfied(logMessageSendProcessor);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }

    @Test
    @DirtiesContext
    public void testPermanentException() throws InterruptedException {
        // Given
        logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
            throw new PermanentException("");
        });
        logMessageSendProcessor.expectedMessageCount(1);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(1);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        // When
        for (int a = 0; a < 1; a++) {
            producerTemplate.sendBodyAndHeaders(Collections.singletonList(
                TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)), ImmutableMap.of());
        }

        // Then
        assertIsSatisfied(logMessageSendProcessor);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }

    @Test
    @DirtiesContext
    public void testTemporaryException() {
        assertThrows(CamelExecutionException.class, () -> {
            // Given
            logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
                throw new TemporaryException("");
            });
            logMessageSendProcessor.expectedMessageCount(1);
            logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
            logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(1);

            // When
            for (int a = 0; a < 1; a++) {
                producerTemplate.sendBodyAndHeaders(
                    Collections.singletonList(TestDataHelper.buildBasePdlLogMessageAsJson(
                        ActivityType.READ)), ImmutableMap.of());
            }

            // Then
            assertIsSatisfied(logMessageSendProcessor);
            assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
            assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
        });
    }

    @Test
    @DirtiesContext
    public void testWebServiceException() throws Exception {

        // Given
        logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
            throw new WebServiceException("");
        });

        logMessageSendProcessor.expectedMessageCount(1);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(1);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        // When
        for (int a = 0; a < 1; a++) {
            producerTemplate.sendBodyAndHeaders(Collections.singletonList(
                TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)), ImmutableMap.of());
        }

        // Then
        assertIsSatisfied(logMessageSendProcessor);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }
}
