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
package se.inera.intyg.logsender.routes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.stereotype.Component;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.exception.BatchValidationException;
import se.inera.intyg.logsender.exception.TemporaryException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogSenderRouteBuilder extends RouteBuilder {
  
  private final LogsenderProperties properties;

  /*
   * This route depends on the MQ provider (currently ActiveMQ) for redelivery. Any temporary exception thrown
   * by any component in this route is NOT handled by the route, but triggers a transaction rollback in the
   * MQ provider. The MQ provider will then, if properly configured, put the message back into the queue after
   * the proper redelivery wait time has passed.
   *
   * Any permanent exception is handled by the route, however, and will NOT trigger a redelivery.
   */
  @Override
  public void configure() {
    errorHandler(defaultErrorHandler().logExhausted(false));

    // 1. Starts by splitting any inbound PdlLogMessage instances having more than one PdlResource into separate
    // PdlLogMessage instances, one per each PdlResource.
    // Then the route Aggregates (n) messages together and passes them to a custom bean which will transform the
    // content into a single list of PdlLogMessage.
    // The bean:logMessageAggregationProcessor outputs a List of PdlLogMessage which is passed to a JMS queue.
    from(properties.queue().receiveLogMessageEndpoint()).routeId("aggregatorRoute")
        .split().method("logMessageSplitProcessor")
        .aggregate(new GroupedExchangeAggregationStrategy())
        .constant(true)
        .completionInterval(properties.aggregation().bulkTimeout())
        .completionPredicate(
            header("CamelAggregatedSize").isEqualTo(properties.aggregation().bulkSize()))
        .to("bean:logMessageAggregationProcessor")
        .to(properties.queue().receiveAggregatedLogMessageEndpoint())
        .stop();

    // 2. In a transaction, reads from jms/AggregatedLogSenderQueue and uses custom bean:logMessageProcessor
    // to convert into ehr:logstore format and send. Exception handling delegates resends to AMQ.
    from(properties.queue().receiveAggregatedLogMessageEndpoint()).routeId(
            "aggregatedJmsToSenderRoute")
        .onException(TemporaryException.class).to("direct:logMessageTemporaryErrorHandlerEndpoint")
        .end()
        .onException(BatchValidationException.class).handled(true)
        .to("direct:logMessageBatchValidationErrorHandlerEndpoint").end()
        .onException(Exception.class).handled(true)
        .to("direct:logMessagePermanentErrorHandlerEndpoint").end()
        .transacted()
        .to("bean:logMessageSendProcessor").stop();

    // Error handling
    from("direct:logMessagePermanentErrorHandlerEndpoint").routeId("permanentErrorLogging")
        .log(LoggingLevel.ERROR, log,
            simple(
                "ENTER - Permanent exception for LogMessage batch: ${exception.message}\n ${exception.stacktrace}")
                .toString())
        .stop();

    from("direct:logMessageBatchValidationErrorHandlerEndpoint").routeId(
            "batchValidationErrorLogging")
        .log(LoggingLevel.ERROR, log,
            simple(
                "ENTER - Batch validation exception for LogMessage batch: ${exception.message}\n ${exception.stacktrace}")
                .toString())
        .to(properties.queue().receiveAggregatedLogMessageDlq())
        .stop();

    from("direct:logMessageTemporaryErrorHandlerEndpoint").routeId("temporaryErrorLogging")
        .choice()
        .when(header("JMSRedelivered").isEqualTo("false"))
        .log(LoggingLevel.ERROR, log,
            simple(
                "ENTER - Temporary exception for logMessage batch: ${exception.message}\n ${exception.stacktrace}")
                .toString())
        .otherwise()
        .log(LoggingLevel.WARN, log,
            simple(
                "ENTER - Temporary exception (redelivered) for logMessage batch: ${exception.message}").toString())
        .stop();
  }
}
