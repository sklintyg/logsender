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
package se.inera.intyg.logsender.route;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
//import org.apache.camel.test.spring.CamelSpringRunner;
//import org.apache.camel.test.spring.CamelTestContextBootstrapper;
//import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.google.common.collect.ImmutableMap;

import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.logsender.config.LogSenderAppConfig;
import se.inera.intyg.logsender.config.LogSenderCamelConfig;
import se.inera.intyg.logsender.helper.TestDataHelper;
//import se.inera.intyg.logsender.testconfig.UnitTestConfig;

@CamelSpringTest
//@ExtendWith(SpringExtension.class)
//@ExtendWith(CamelTestSupport.class)
//@RunWith(CamelSpringRunner.class)
@TestPropertySource("classpath:logsender/unit-test.properties")
@ContextConfiguration(classes = {LogSenderAppConfig.class}, loader = AnnotationConfigContextLoader.class)
//@ComponentScan(basePackages = "se.inera.intyg.logsender")
//@BootstrapWith(CamelTestContextBootstrapper.class)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class}) // Suppresses warning
@MockEndpointsAndSkip("bean:logMessageAggregationProcessor|direct:logMessagePermanentErrorHandlerEndpoint|direct:logMessageTemporaryErrorHandlerEndpoint|direct:receiveAggregatedLogMessageEndpoint")
//@MockEndpointsAndSkip("bean:logMessageAggregationProcessor|direct:logMessagePermanentErrorHandlerEndpoint|direct:logMessageTemporaryErrorHandlerEndpoint|direct:receiveAggregatedLogMessageEndpoint")
public class AggregatorRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Produce("direct://receiveLogMessageEndpoint")
    protected ProducerTemplate producerTemplate;

    @EndpointInject("mock:bean:logMessageAggregationProcessor")
    protected MockEndpoint logMessageAggregationProcessor;

    @EndpointInject("mock:direct:receiveAggregatedLogMessageEndpoint")
    protected MockEndpoint newAggregatedLogMessageQueue;

    @EndpointInject("mock:direct:logMessagePermanentErrorHandlerEndpoint")
    protected MockEndpoint logMessagePermanentErrorHandlerEndpoint;

    @EndpointInject("mock:direct:logMessageTemporaryErrorHandlerEndpoint")
    protected MockEndpoint logMessageTemporaryErrorHandlerEndpoint;

    @BeforeEach
    public void setup() {
        MockEndpoint.resetMocks(camelContext);
    }

    @Test
    @DirtiesContext
    public void testNormalLogStoreRoute() throws InterruptedException {
        // Given
        logMessageAggregationProcessor.expectedMessageCount(1);
        newAggregatedLogMessageQueue.expectedMessageCount(1);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        // When
        for (int a = 0; a < 5; a++) {
            producerTemplate
                //.sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.<String, Object>of());
                .sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.of());

        }

        // Then
        assertIsSatisfied(logMessageAggregationProcessor);
        assertIsSatisfied(newAggregatedLogMessageQueue);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }

    @Test
    @DirtiesContext
    public void testNoMessagesReceivedWhenMessageCountLessThanBatchSize() throws InterruptedException {
        // Given
        logMessageAggregationProcessor.expectedMessageCount(0);
        newAggregatedLogMessageQueue.expectedMessageCount(0);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        // When
        for (int a = 0; a < 4; a++) {
            producerTemplate
                //.sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.<String, Object>of());
                .sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.of());

        }

        // Then
        assertIsSatisfied(logMessageAggregationProcessor);
        assertIsSatisfied(newAggregatedLogMessageQueue);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }
}
