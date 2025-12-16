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
package se.inera.intyg.logsender.integrationtest.util;

import jakarta.jms.Queue;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;

@TestConfiguration
@EnableAutoConfiguration
public class IntegrationTestConfig {

  @Bean
  @Primary
  public MockLogSenderClientImpl mockSendCertificateServiceClient() {
    return new MockLogSenderClientImpl();
  }

  @Bean
  public Queue newLogMessageQueue() {
    final var newLogMessageQueue = new ActiveMQQueue();
    newLogMessageQueue.setPhysicalName("newLogMessageQueue");
    return newLogMessageQueue;
  }

  @Bean
  public Queue newAggregatedLogMessageQueue() {
    final var newAggregatedLogMessageQueue = new ActiveMQQueue();
    newAggregatedLogMessageQueue.setPhysicalName("newAggregatedLogMessageQueue");
    return newAggregatedLogMessageQueue;
  }

  @Bean
  public Queue newAggregatedLogMessageDLQ() {
    final var newAggregatedLogMessageDLQ = new ActiveMQQueue();
    newAggregatedLogMessageDLQ.setPhysicalName("DLQ.newAggregatedLogMessageQueue");
    return newAggregatedLogMessageDLQ;
  }

  @Bean
  public ActiveMQConnectionFactoryCustomizer redeliveryPolicyCustomizer() {
    return factory -> {
      final var policy = new RedeliveryPolicy();
      policy.setMaximumRedeliveries(1);
      policy.setInitialRedeliveryDelay(100);
      policy.setUseExponentialBackOff(true);
      policy.setBackOffMultiplier(2);
      policy.setMaximumRedeliveryDelay(5000);
      factory.setRedeliveryPolicy(policy);
      factory.setNonBlockingRedelivery(true);
    };
  }
}