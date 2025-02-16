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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.inera.intyg.common.util.integration.json.CustomObjectMapper;
import se.inera.intyg.infra.logmessages.PdlLogMessage;
import se.inera.intyg.infra.logmessages.PdlResource;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.logging.MdcCloseableMap;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.logging.MdcLogConstants;

/**
 * This camel processor implements the split pattern. It will create a new Message for each {@link PdlResource} within the deserialized
 * {@link PdlLogMessage}.
 *
 * The reason for this is that the PDL-service does not accept multiple PdlResources for different patients bound to the same
 * PdlLogMessage.
 *
 * Created by eriklupander on 2016-03-16.
 */
@RequiredArgsConstructor
public class LogMessageSplitProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LogMessageSplitProcessor.class);

    private final ObjectMapper objectMapper = new CustomObjectMapper();

    private final MdcHelper mdcHelper;

    /**
     * If a PdlLogMessage contains more than one resource, it is split into (n resources) number of new PdlLogMessages with one Resource
     * each.
     *
     * @param body The inbound message, typically containing a JSON-serialized {@link PdlLogMessage} as body.
     * @return A list of {@link DefaultMessage} where each message contains a body of one {@link PdlLogMessage} having exactly one {@link
     * PdlResource}.
     */
    public List<Message> process(@Body Message body) throws IOException, PermanentException {
        try (MdcCloseableMap mdc = MdcCloseableMap.builder()
            .put(MdcLogConstants.TRACE_ID_KEY, mdcHelper.traceId())
            .put(MdcLogConstants.SPAN_ID_KEY, mdcHelper.spanId())
            .build()
        ) {
            List<Message> answer = new ArrayList<>();

            if (body != null) {
                PdlLogMessage pdlLogMessage = objectMapper.readValue((String) body.getBody(), PdlLogMessage.class);
                if (pdlLogMessage.getPdlResourceList().isEmpty()) {
                    LOG.error("No resources in PDL log message {}, not proceeding.", pdlLogMessage.getLogId());
                    throw new PermanentException("No resources in PDL log message, discarding message.");
                } else if (pdlLogMessage.getPdlResourceList().size() == 1) {
                    answer.add(body);
                } else {
                    splitIntoOnePdlLogMessagePerResource(answer, pdlLogMessage);
                }
            }
            return answer;
        }
    }

    private void splitIntoOnePdlLogMessagePerResource(List<Message> answer, PdlLogMessage pdlLogMessage) throws JsonProcessingException {
        for (PdlResource resource : pdlLogMessage.getPdlResourceList()) {
            PdlLogMessage copiedPdlLogMsg = pdlLogMessage.copy(false);
            copiedPdlLogMsg.getPdlResourceList().add(resource);

            DefaultMessage message = new DefaultMessage(new DefaultCamelContext());
            message.setBody(objectMapper.writeValueAsString(copiedPdlLogMsg));
            answer.add(message);
        }
    }
}
