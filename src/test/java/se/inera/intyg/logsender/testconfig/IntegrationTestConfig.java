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
package se.inera.intyg.logsender.testconfig;

import jakarta.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;
import se.inera.intyg.logsender.config.LogSenderBeanConfig;

@Lazy
@Configuration
@Import({LogSenderBeanConfig.class, IntegrationTestJmsConfig.class, IntegrationTestBrokerService.class})
@PropertySource({"classpath:application.properties", "classpath:logsender/integration-test.properties"})
@ImportResource({"classpath:camel-context.xml", "classpath:/basic-cache-config.xml", "classpath:/loggtjanst-stub-context.xml"})
public class IntegrationTestConfig {

    @Bean
    public MockLogSenderClientImpl mockSendCertificateServiceClient() {
        return new MockLogSenderClientImpl();
    }

    @Bean
    public Queue newLogMessageQueue() {
        ActiveMQQueue newLogMessageQueue = new ActiveMQQueue();
        newLogMessageQueue.setPhysicalName("newLogMessageQueue");
        return newLogMessageQueue;
    }

    @Bean
    public Queue newAggregatedLogMessageQueue() {
        ActiveMQQueue newAggregatedLogMessageQueue = new ActiveMQQueue();
        newAggregatedLogMessageQueue.setPhysicalName("newAggregatedLogMessageQueue");
        return newAggregatedLogMessageQueue;
    }

    @Bean
    public Queue newAggregatedLogMessageDLQ() {
        ActiveMQQueue newAggregatedLogMessageDLQ = new ActiveMQQueue();
        newAggregatedLogMessageDLQ.setPhysicalName("DLQ.newAggregatedLogMessageQueue");
        return newAggregatedLogMessageDLQ;
    }
}
