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
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.inera.intyg.logsender.exception.PermanentException;
import se.inera.intyg.logsender.logging.MdcCloseableMap;
import se.inera.intyg.logsender.logging.MdcHelper;
import se.inera.intyg.logsender.logging.MdcLogConstants;

/**
 * Accepts a Camel Exchange that must contain a {@link Exchange#GROUPED_EXCHANGE} of (n) log messages that should be sent in a batch to the
 * PDL-log service.
 *
 * The resulting list of {@link PdlLogMessage} is serialized into a JSON string and passed on so Camel can supply it to the next consumer.
 *
 * The next consumer is typically the aggreagated.jms.queue. Since we want TextMessages for readability, the conversion to a JSON string is
 * performed.
 *
 * Created by eriklupander on 2016-02-29.
 */
@RequiredArgsConstructor
public class LogMessageAggregationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LogMessageAggregationProcessor.class);

    private final ObjectMapper objectMapper;

    private final MdcHelper mdcHelper;

    /**
     * Transforms the contents of the grouped exchange into a list of {@link PdlLogMessage}.
     *
     * @param exchange An exchange typically containing (n) number of exchanges that has been aggregated into a grouped exchange.
     * @return An List<String>. Note that the payload is JSON, simplifies readability if message ever ends up on a DLQ.
     * @throws PermanentException If the exchange could not be read or did not contain any grouped exchanges, just ignore.
     */
    public String process(Exchange exchange) throws PermanentException, JsonProcessingException {
        try (MdcCloseableMap mdc = MdcCloseableMap.builder()
            .put(MdcLogConstants.TRACE_ID_KEY, mdcHelper.traceId())
            .put(MdcLogConstants.SPAN_ID_KEY, mdcHelper.spanId())
            .build()
        ) {
            List<Exchange> grouped = exchange.getIn().getBody(List.class);

            if (grouped == null || grouped.isEmpty()) {
                LOG.info("No aggregated log messages, this is normal if camel aggregator has a batch timeout. Doing nothing.");
                throw new PermanentException("No aggregated messages, no reason to retry");
            }

            List<String> aggregatedList = grouped.stream()
                .map(oneExchange -> (String) oneExchange.getIn().getBody())
                .toList();

            return objectMapper.writeValueAsString(aggregatedList);
        }
    }
}
