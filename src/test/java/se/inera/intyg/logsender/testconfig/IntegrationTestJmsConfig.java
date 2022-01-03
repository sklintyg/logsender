/*
 * Copyright (C) 2022 Inera AB (http://www.inera.se)
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
package se.inera.intyg.logsender.testconfig;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class IntegrationTestJmsConfig {

    @Value("${errorhandling.maxRedeliveries}")
    private int maximumRedeliveries;

    @Value("${errorhandling.maxRedeliveryDelay}")
    private int maximumRedeliveryDelay;

    @Value("${errorhandling.redeliveryDelay}")
    private int initialRedeliveryDelay;

    @Value("${testBrokerUrl}")
    private String testBrokerUrl;

    @Autowired
    CamelContext camelContext;

    @Bean(name = "jms")
    public ActiveMQComponent integrationActiveMQComponent() {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConnectionFactory(cachingConnectionFactory());
        activeMQComponent.setTransactionManager(jmsTransactionManager());
        activeMQComponent.setTransacted(true);
        activeMQComponent.setCacheLevelName("CACHE_CONSUMER");
        activeMQComponent.setCamelContext(camelContext);
        return activeMQComponent;
    }

    @Bean
    public CachingConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(jmsConnectionFactory());
        return cachingConnectionFactory;
    }

    private JmsTransactionManager jmsTransactionManager() {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(cachingConnectionFactory());
        return jmsTransactionManager;
    }

    private ActiveMQConnectionFactory jmsConnectionFactory() {
        ActiveMQConnectionFactory jmsConnectionFactory = new ActiveMQConnectionFactory();
        jmsConnectionFactory.setBrokerURL(testBrokerUrl);
        jmsConnectionFactory.setRedeliveryPolicy(redeliveryPolicy());
        jmsConnectionFactory.setNonBlockingRedelivery(true);
        return jmsConnectionFactory;
    }

    private RedeliveryPolicy redeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(maximumRedeliveries);
        redeliveryPolicy.setMaximumRedeliveryDelay(maximumRedeliveryDelay);
        redeliveryPolicy.setInitialRedeliveryDelay(initialRedeliveryDelay);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setBackOffMultiplier(2);
        return redeliveryPolicy;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(cachingConnectionFactory());
        return jmsTemplate;
    }

    @Bean
    public SpringTransactionPolicy policy() {
        SpringTransactionPolicy policy = new SpringTransactionPolicy();
        policy.setTransactionManager(jmsTransactionManager());
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return policy;
    }
}
