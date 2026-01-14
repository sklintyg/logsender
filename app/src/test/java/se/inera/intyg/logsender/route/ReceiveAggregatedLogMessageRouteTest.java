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
package se.inera.intyg.logsender.route;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import jakarta.xml.ws.WebServiceException;
import java.util.Collections;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.exception.TemporaryException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.testconfig.UnitTestConfig;

@SpringBootTest(classes = UnitTestConfig.class)
@ActiveProfiles({"test", "testability"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class ReceiveAggregatedLogMessageRouteTest {

  @Autowired
  private CamelContext camelContext;

  @Autowired
  private ProducerTemplate producerTemplate;

  @EndpointInject("mock:bean:logMessageSendProcessor")
  MockEndpoint logMessageSendProcessor;

  @EndpointInject("mock:direct:logMessagePermanentErrorHandlerEndpoint")
  MockEndpoint logMessagePermanentErrorHandlerEndpoint;

  @EndpointInject("mock:direct:logMessageTemporaryErrorHandlerEndpoint")
  MockEndpoint logMessageTemporaryErrorHandlerEndpoint;

  @BeforeEach
  void setup() throws Exception {
    MockEndpoint.resetMocks(camelContext);
    AdviceWith.adviceWith(camelContext, "aggregatedJmsToSenderRoute", in ->
        in.mockEndpointsAndSkip("direct:logMessageTemporaryErrorHandlerEndpoint",
            "bean:logMessageSendProcessor", "direct:logMessagePermanentErrorHandlerEndpoint"));
    camelContext.start();
  }

  @Test
  void testNormalLogStoreRoute() throws InterruptedException {
    logMessageSendProcessor.expectedMessageCount(1);
    logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
    logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

    producerTemplate.sendBodyAndHeaders("direct:receiveAggregatedLogMessageEndpoint",
        Collections.singletonList(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)),
        ImmutableMap.of());

    assertIsSatisfied(logMessageSendProcessor);
    assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
    assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
  }

  @Test
  void testPermanentException() throws InterruptedException {
    logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
      throw new PermanentException("");
    });

    logMessageSendProcessor.expectedMessageCount(1);
    logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(1);
    logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(0);

    producerTemplate.sendBodyAndHeaders("direct:receiveAggregatedLogMessageEndpoint",
        Collections.singletonList(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)),
        ImmutableMap.of());

    assertIsSatisfied(logMessageSendProcessor);
    assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
    assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
  }

  @Test
  void testTemporaryException() throws InterruptedException {
    logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
      throw new TemporaryException("");
    });

    logMessageSendProcessor.expectedMessageCount(1);
    logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(0);
    logMessageTemporaryErrorHandlerEndpoint.expectedMessageCount(1);

    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ);

    assertThrows(CamelExecutionException.class, () ->
        producerTemplate.sendBodyAndHeaders("direct:receiveAggregatedLogMessageEndpoint",
            Collections.singletonList(pdlLogMessage),
            ImmutableMap.of()));

    assertIsSatisfied(logMessageSendProcessor);
    assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
    assertIsSatisfied(logMessageTemporaryErrorHandlerEndpoint);
  }

  @Test
  void testWebServiceException() throws Exception {
    logMessageSendProcessor.whenAnyExchangeReceived(exchange -> {
      throw new WebServiceException("");
    });

    logMessageSendProcessor.expectedMessageCount(1);
    logMessagePermanentErrorHandlerEndpoint.expectedMessageCount(1);

    producerTemplate.sendBodyAndHeaders("direct:receiveAggregatedLogMessageEndpoint",
        Collections.singletonList(TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ)),
        ImmutableMap.of());

    assertIsSatisfied(logMessageSendProcessor);
    assertIsSatisfied(logMessagePermanentErrorHandlerEndpoint);
  }
}
