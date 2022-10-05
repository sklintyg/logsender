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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.ImmutableMap;

import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.testconfig.UnitTestConfig;

@CamelSpringTest
@TestPropertySource(locations = "classpath:logsender/unit-test.properties")
@ContextConfiguration(classes = {UnitTestConfig.class})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
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
    public void setup() throws Exception {
        AdviceWith.adviceWith(camelContext, "aggregatorRoute", in ->
            in.mockEndpointsAndSkip("bean:logMessageAggregationProcessor", "direct:receiveAggregatedLogMessageEndpoint",
                "direct:logMessageTemporaryErrorHandlerEndpoint", "direct:logMessagePermanentErrorHandlerEndpoint"));
    }

    @Test
    public void testNormalLogStoreRoute() throws InterruptedException {
        logMessageAggregationProcessor.expectedMessageCount(1);
        newAggregatedLogMessageQueue.expectedMessageCount(1);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        for (int a = 0; a < 5; a++) {
            producerTemplate
                .sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.of());
        }

        assertIsSatisfied(logMessageAggregationProcessor);
        assertIsSatisfied(newAggregatedLogMessageQueue);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }

    @Test
    public void testNoMessagesReceivedWhenMessageCountLessThanBatchSize() throws InterruptedException {
        logMessageAggregationProcessor.expectedMessageCount(0);
        newAggregatedLogMessageQueue.expectedMessageCount(0);
        logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
        logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

        for (int a = 0; a < 4; a++) {
            producerTemplate
                .sendBodyAndHeaders(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ), ImmutableMap.of());
        }

        assertIsSatisfied(logMessageAggregationProcessor);
        assertIsSatisfied(newAggregatedLogMessageQueue);
        assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
        assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
    }
}
