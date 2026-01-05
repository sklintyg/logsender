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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.springframework.stereotype.Component;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.logging.MdcCloseableMap;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.logging.MdcLogConstants;
import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.inera.intyg.logsender.model.PdlResource;


@Component
@RequiredArgsConstructor
@Slf4j
public class LogMessageSplitProcessor {

  private final ObjectMapper objectMapper = new CustomObjectMapper();

  public List<Message> process(@Body Message body) throws IOException, PermanentException {
    try (MdcCloseableMap ignored = MdcCloseableMap.builder()
        .put(MdcLogConstants.TRACE_ID_KEY, MdcHelper.traceId())
        .put(MdcLogConstants.SPAN_ID_KEY, MdcHelper.spanId())
        .build()
    ) {
      final var answer = new ArrayList<Message>();

      if (body != null) {
        final var pdlLogMessage = objectMapper.readValue((String) body.getBody(),
            PdlLogMessage.class);
        if (pdlLogMessage.getPdlResourceList().isEmpty()) {
          log.error("No resources in PDL log message {}, not proceeding.",
              pdlLogMessage.getLogId());
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

  private void splitIntoOnePdlLogMessagePerResource(List<Message> answer,
      PdlLogMessage pdlLogMessage) throws JsonProcessingException {
    for (PdlResource resource : pdlLogMessage.getPdlResourceList()) {
      final var copiedPdlLogMsg = pdlLogMessage.copy(false);
      copiedPdlLogMsg.getPdlResourceList().add(resource);

      final var message = new DefaultMessage(new DefaultCamelContext());
      message.setBody(objectMapper.writeValueAsString(copiedPdlLogMsg));
      answer.add(message);
    }
  }
}
