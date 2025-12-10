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
package se.inera.intyg.logsender.config;

import static org.apache.camel.LoggingLevel.OFF;

import jakarta.jms.ConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

@Configuration
public class LogSenderJmsConfig {

  @Autowired
  private CamelContext camelContext;

  @Autowired
  @Qualifier("jmsConnectionFactory")
  private ConnectionFactory connectionFactory;

  @Bean(name = "jms")
  public ActiveMQComponent activeMQComponent() {
    ActiveMQComponent activeMQComponent = new ActiveMQComponent();
    activeMQComponent.setConnectionFactory(connectionFactory);
    activeMQComponent.setConfiguration(jmsConfiguration());
    activeMQComponent.setTransacted(true);
    activeMQComponent.setCacheLevelName("CACHE_CONSUMER");
    activeMQComponent.setCamelContext(camelContext);
    return activeMQComponent;
  }

  private JmsConfiguration jmsConfiguration() {
    JmsConfiguration jmsConfig = new JmsConfiguration();
    jmsConfig.setConnectionFactory(connectionFactory);
    jmsConfig.setErrorHandlerLoggingLevel(OFF);
    jmsConfig.setErrorHandlerLogStackTrace(false);
    jmsConfig.setDestinationResolver(jmsDestinationResolver());
    return jmsConfig;
  }

  private DynamicDestinationResolver jmsDestinationResolver() {
    return new DynamicDestinationResolver();
  }
}