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

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.Test;
import java.util.List;

import org.apache.camel.Message;

import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.helper.TestDataHelper;

/**
 * Created by eriklupander on 2016-03-16.
 */
public class LogMessageSplitProcessorTest {

    private LogMessageSplitProcessor testee = new LogMessageSplitProcessor();

    @Test
    public void testSingleResource() throws Exception {
        List<Message> messages = testee.process(buildMessage(1));
        assertEquals(1, messages.size());
    }

    @Test
    public void testMultipleResources() throws Exception {
        List<Message> messages = testee.process(buildMessage(3));
        assertEquals(3, messages.size());
    }

    @Test
    public void testNoResource() {
        assertThrows(PermanentException.class, () ->
            testee.process(buildMessage(0)));
    }

    private Message buildMessage(int numberOfResources) {
        DefaultMessage msg = new DefaultMessage(new DefaultCamelContext());
        msg.setBody(buildBody(numberOfResources));
        return msg;
    }

    private String buildBody(int numberOfResources) {
        return TestDataHelper.buildBasePdlLogMessageAsJson(ActivityType.READ, numberOfResources);
    }
}
