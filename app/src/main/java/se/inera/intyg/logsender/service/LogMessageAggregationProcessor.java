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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.logging.MdcCloseableMap;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.logging.MdcLogConstants;


@Component
@RequiredArgsConstructor
@Slf4j
public class LogMessageAggregationProcessor {


  private final ObjectMapper objectMapper;

  private final MdcHelper mdcHelper;


  public String process(Exchange exchange) throws PermanentException, JsonProcessingException {
    try (MdcCloseableMap ignored = MdcCloseableMap.builder()
        .put(MdcLogConstants.TRACE_ID_KEY, mdcHelper.traceId())
        .put(MdcLogConstants.SPAN_ID_KEY, mdcHelper.spanId())
        .build()
    ) {
      List<Exchange> grouped = exchange.getIn().getBody(List.class);

      if (grouped == null || grouped.isEmpty()) {
        log.info(
            "No aggregated log messages, this is normal if camel aggregator has a batch timeout. Doing nothing.");
        throw new PermanentException("No aggregated messages, no reason to retry");
      }

      List<String> aggregatedList = grouped.stream()
          .map(oneExchange -> (String) oneExchange.getIn().getBody())
          .toList();

      return objectMapper.writeValueAsString(aggregatedList);
    }
  }
}
