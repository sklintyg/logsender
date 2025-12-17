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
package se.inera.intyg.logsender.standalone;

import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.helper.ValueInclude;

/**
 * Stand-alone "application" that can connect to a local running ActiveMQ and send a PdlLogMessage
 * (as json) to localhost:61616. Useful for debugging / troubleshooting purposes when you don't want
 * to start a Webcert or Rehabstod instance.
 * <p>
 * The message is created using the
 * {@link TestDataHelper#buildBasePdlLogMessage(ActivityType, ValueInclude, ValueInclude)}
 */
public class SimpleLogMessageSender {

  private static final String URL = ActiveMQConnection.DEFAULT_BROKER_URL;

  public static void main(String[] args) throws JMSException, JsonProcessingException {
    final var connectionFactory = new ActiveMQConnectionFactory(URL);
    final var connection = connectionFactory.createConnection();
    connection.start();

    final var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    final var subject = "dev.logging.queue";
    final var destination = session.createQueue(subject);
    final var producer = session.createProducer(destination);

    final var pdlLogMessage = TestDataHelper
        .buildBasePdlLogMessage(ActivityType.CREATE, ValueInclude.INCLUDE, ValueInclude.INCLUDE);
    final var message = session.createTextMessage(
        new CustomObjectMapper().writeValueAsString(pdlLogMessage));
    producer.send(message);
    System.out.println("Sent message: '" + message.getText() + "'");

    connection.close();
  }

}
