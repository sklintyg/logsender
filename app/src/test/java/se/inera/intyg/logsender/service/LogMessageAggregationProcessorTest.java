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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.inera.intyg.logsender.helper.TestDataHelper.OBJECT_MAPPER;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.model.ActivityType;

@ExtendWith(MockitoExtension.class)
class LogMessageAggregationProcessorTest {

  private LogMessageAggregationProcessor logMessageAggregationProcessor;

  @BeforeEach
  void setUp() {
    logMessageAggregationProcessor = new LogMessageAggregationProcessor(OBJECT_MAPPER);
  }

  @Test
  void testOkGroupedExchange() throws Exception {
    final var body = logMessageAggregationProcessor.process(buildGroupedExchange(1, 1));
    final var output = OBJECT_MAPPER.readValue(body, ArrayList.class);
    assertEquals(1, output.size());
  }

  @Test
  void testGroupedExchangeWithMultipleResources() throws Exception {
    final var body = logMessageAggregationProcessor.process(buildGroupedExchange(3, 5));
    final var output = OBJECT_MAPPER.readValue(body, ArrayList.class);
    assertEquals(3, output.size());
  }

  @Test
  void testEmptyGroupedExchange() {
    assertThrows(PermanentException.class, () ->
        logMessageAggregationProcessor.process(buildGroupedExchange(0, 1)));
  }


  private Exchange buildGroupedExchange(int exchangeSize, int resourcesPerMessageSize) {
    final var exchange = mock(Exchange.class);
    final var outerMessage = buildOuterMsg(exchangeSize, resourcesPerMessageSize);
    when(exchange.getIn()).thenReturn(outerMessage);
    return exchange;
  }

  private Message buildOuterMsg(int exchangeSize, int resourcesPerMessageSize) {
    final var outerMsg = mock(Message.class);
    final var outerBody = buildOuterBody(exchangeSize, resourcesPerMessageSize);
    when(outerMsg.getBody(List.class)).thenReturn(outerBody);
    return outerMsg;
  }

  private List<Exchange> buildOuterBody(int exchangeSize, int resourcesPerMessageSize) {
    List<Exchange> groupedMessages = new ArrayList<>();
    for (int i = 0; i < exchangeSize; i++) {
      final var innerExchange = mock(Exchange.class);
      final var innerMessage = buildInnerMessage(resourcesPerMessageSize);
      when(innerExchange.getIn()).thenReturn(innerMessage);
      groupedMessages.add(innerExchange);
    }
    return groupedMessages;
  }

  private Message buildInnerMessage(int resourcesPerMessageSize) {
    final var innerMessage = mock(Message.class);
    when(innerMessage.getBody()).thenReturn(pdlLogMessageJson(resourcesPerMessageSize));
    return innerMessage;
  }

  private String pdlLogMessageJson(int resourcesPerMessageSize) {
    return TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ, resourcesPerMessageSize);
  }
}
