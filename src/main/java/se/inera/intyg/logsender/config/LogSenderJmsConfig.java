/*
 * Copyright (C) 2020 Inera AB (http://www.inera.se)
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

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;


import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class LogSenderJmsConfig {

    @Value("${activemq.broker.username}")
    private String activemqBrokerUsername;

    @Value("${activemq.broker.password}")
    private String activemqBrokerPassword;

    @Value("${activemq.broker.url}")
    private String activemqBrokerUrl;

    @Bean
    public JmsTransactionManager jmsTransactionManager() {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(transactionAwareConnectionFactoryProxy());
        return jmsTransactionManager;
    }

    @Bean
    public ActiveMQComponent activeMQComponent() {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConnectionFactory(transactionAwareConnectionFactoryProxy());
        activeMQComponent.setConfiguration(jmsConfiguration());
        activeMQComponent.setTransacted(true);
        activeMQComponent.setCacheLevelName("CACHE_CONSUMER");
        return activeMQComponent;
    }

    @Bean
    public JmsConfiguration jmsConfiguration() {
        JmsConfiguration jmsConfig = new JmsConfiguration();
        jmsConfig.setConnectionFactory(transactionAwareConnectionFactoryProxy());
        jmsConfig.setErrorHandlerLoggingLevel(OFF);
        jmsConfig.setErrorHandlerLogStackTrace(false);
        jmsConfig.setDestinationResolver(jmsDestinationResolver());
        return jmsConfig;
    }

    @Bean
    public DynamicDestinationResolver jmsDestinationResolver() {
        return new DynamicDestinationResolver();
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory jmsFactory = new ActiveMQConnectionFactory();
        jmsFactory.setUserName(activemqBrokerUsername);
        jmsFactory.setPassword(activemqBrokerPassword);
        jmsFactory.setBrokerURL(activemqBrokerUrl);
        return jmsFactory;
    }

    @Bean
    public TransactionAwareConnectionFactoryProxy transactionAwareConnectionFactoryProxy() {
        TransactionAwareConnectionFactoryProxy cachingConnectionFactory
            = new TransactionAwareConnectionFactoryProxy();
        cachingConnectionFactory.setTargetConnectionFactory(connectionFactory());
        cachingConnectionFactory.setSynchedLocalTransactionAllowed(true);
        return cachingConnectionFactory;
    }
/*
    // Added SpringTransactionPolicy to make jmsTransactionManger of this class take precedence over the
    // MockTransactionManager used in tests. The duplicate transaction managers caused conflicts in
    // TransactionErrorHandlerBuilder of file LogSenderRouteBuilder otherwise.
    @Bean
    public SpringTransactionPolicy policy() {
        SpringTransactionPolicy policy = new SpringTransactionPolicy(jmsTransactionManager());
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return policy;
    }

 */
}
