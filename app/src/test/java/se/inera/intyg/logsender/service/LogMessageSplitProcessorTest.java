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
import static org.mockito.Mockito.when;

import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.model.ActivityType;

@ExtendWith(MockitoExtension.class)
class LogMessageSplitProcessorTest {

  @Mock
  MdcHelper mdcHelper;

  private LogMessageSplitProcessor logMessageSplitProcessor;

  @BeforeEach
  void setUp() {
    when(mdcHelper.spanId()).thenReturn("spanId");
    when(mdcHelper.traceId()).thenReturn("traceId");

    logMessageSplitProcessor = new LogMessageSplitProcessor(mdcHelper);
  }

  @Test
  void testSingleResource() throws Exception {
    final var messages = logMessageSplitProcessor.process(buildMessage(1));
    assertEquals(1, messages.size());
  }

  @Test
  void testMultipleResources() throws Exception {
    final var messages = logMessageSplitProcessor.process(buildMessage(3));
    assertEquals(3, messages.size());
  }

  @Test
  void testNoResource() {
    assertThrows(PermanentException.class, () ->
        logMessageSplitProcessor.process(buildMessage(0)));
  }

  private Message buildMessage(int numberOfResources) {
    final var msg = new DefaultMessage(new DefaultCamelContext());
    msg.setBody(buildBody(numberOfResources));
    return msg;
  }

  private String buildBody(int numberOfResources) {
    return TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ, numberOfResources);
  }
}
