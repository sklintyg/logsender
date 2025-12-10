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
package se.inera.intyg.logsender.testconfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.policy.DeadLetterStrategy;
import org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationTestBrokerService {

  @Value("${spring.activemq.broker-url}")
  private String brokerUrl;

  @Bean
  public BrokerService integrationTestBrokerService() throws Exception {
    BrokerService brokerService = new BrokerService();
    brokerService.setPersistent(false);
    brokerService.setDeleteAllMessagesOnStartup(true);
    brokerService.setDestinationPolicy(getPolicyMap());
    brokerService.setTransportConnectors(getTransportConnectors());
    return brokerService;
  }

  private PolicyMap getPolicyMap() {
    PolicyMap policyMap = new PolicyMap();
    policyMap.setPolicyEntries(getPolicyEntries());
    return policyMap;
  }

  private List<PolicyEntry> getPolicyEntries() {
    PolicyEntry policyEntry = new PolicyEntry();
    policyEntry.setQueue("newAggregatedLogMessageQueue");
    policyEntry.setDeadLetterStrategy(deadLetterStrategy());
    return Collections.singletonList(policyEntry);
  }

  private DeadLetterStrategy deadLetterStrategy() {
    IndividualDeadLetterStrategy iDeadLetterStrategy = new IndividualDeadLetterStrategy();
    iDeadLetterStrategy.setQueuePrefix("DLQ.");
    iDeadLetterStrategy.setUseQueueForQueueMessages(true);
    return iDeadLetterStrategy;
  }

  private List<TransportConnector> getTransportConnectors() throws URISyntaxException {
    TransportConnector transportConnector = new TransportConnector();
    transportConnector.setUri(new URI(brokerUrl));
    return Collections.singletonList(transportConnector);
  }
}
