/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
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

import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.model.ActivityType;

@ExtendWith(MockitoExtension.class)
class LogMessageSplitProcessorTest {

  private LogMessageSplitProcessor logMessageSplitProcessor;

  @BeforeEach
  void setUp() {
    logMessageSplitProcessor = new LogMessageSplitProcessor();
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
