/*
 * Copyright (C) 2017 Inera AB (http://www.inera.se)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import se.inera.intyg.common.util.integration.json.CustomObjectMapper;
import se.inera.intyg.infra.logmessages.ActivityType;
import se.inera.intyg.infra.logmessages.PdlLogMessage;
import se.inera.intyg.logsender.helper.PatientNameInclude;
import se.inera.intyg.logsender.helper.TestDataHelper;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Stand-alone "application" that can connect to a local running ActiveMQ and send
 * a PdlLogMessage (as json) to localhost:61616. Useful for debugging / troubleshooting
 * purposes when you don't want to start a Webcert or Rehabstod instance.
 *
 * The message is created using the {@link TestDataHelper#buildBasePdlLogMessage(ActivityType,PatientNameInclude)}
 *
 * Created by eriklupander on 2017-10-24.
 */
public class SimpleLogMessageSender {

    private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;

    private static String subject = "dev.logging.queue"; // Queue Name

    public static void main(String[] args) throws JMSException, JsonProcessingException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false,
                Session.AUTO_ACKNOWLEDGE);

        Destination destination = session.createQueue(subject);
        MessageProducer producer = session.createProducer(destination);

        PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.CREATE, PatientNameInclude.INCLUDE);
        TextMessage message = session.createTextMessage(new CustomObjectMapper().writeValueAsString(pdlLogMessage));
        // Here we are sending the message!
        producer.send(message);
        System.out.println("Sent message: '" + message.getText() + "'");

        connection.close();
    }

}
