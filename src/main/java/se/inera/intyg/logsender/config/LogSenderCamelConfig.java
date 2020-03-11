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



import com.helger.commons.annotation.Singleton;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.inera.intyg.logsender.routes.LogSenderRouteBuilder;

@Configuration
public class LogSenderCamelConfig extends CamelConfiguration {

    @Autowired
    ActiveMQComponent activeMQComponent;

    @Autowired
    JmsConfiguration jmsConfiguration;

    @Autowired
    LogSenderRouteBuilder logSenderRouteBuilder;

    @Value("${receiveLogMessageEndpointUri}")
    String receiveLogMessageEndpointUri;

    @Value("${receiveAggregatedLogMessageEndpointUri}")
    String receiveAggregatedLogMessageEndpointUri;
/*
    @Bean
    public CamelEndpointFactoryBean receiveLogMessageEndpoint() {
        CamelEndpointFactoryBean receiveLogMessageEndpoint = new CamelEndpointFactoryBean();
        receiveLogMessageEndpoint.setId("receiveLogMessageEndpoint");
        receiveLogMessageEndpoint.setUri(receiveLogMessageEndpointUri);
        return receiveLogMessageEndpoint;
    }

    @Bean
    public CamelEndpointFactoryBean receiveAggregatedLogMessageEndpoint() {
        CamelEndpointFactoryBean receiveAggregatedLogMessageEndpoint = new CamelEndpointFactoryBean();
        receiveAggregatedLogMessageEndpoint.setId("receiveAggregatedLogMessageEndpoint");
        receiveAggregatedLogMessageEndpoint.setUri(receiveAggregatedLogMessageEndpointUri);
        receiveAggregatedLogMessageEndpoint.consu
        return receiveAggregatedLogMessageEndpoint;
    }
*/
    //@Bean
/*    private JmsEndpoint receiveLogMessageEndpoint() {
        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setConfiguration(jmsConfiguration);
        jmsEndpoint.setDestinationName(receiveLogMessageEndpointUri);
        return jmsEndpoint;
    }


    //@Bean
    private JmsEndpoint receiveAggregatedLogMessageEndpoint() {
        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setConfiguration(jmsConfiguration);
        jmsEndpoint.setDestinationName(receiveAggregatedLogMessageEndpointUri);
        return jmsEndpoint;
    }
*/
    @Bean
    public JmsEndpoint receiveLogMessageEndpoint() {
        JmsEndpoint jmsEndpoint = new JmsEndpoint(receiveLogMessageEndpointUri,
            activeMQComponent, receiveLogMessageEndpointUri, true, jmsConfiguration);
        return  jmsEndpoint;
    }

    @Bean
    public JmsEndpoint receiveAggregatedLogMessageEndpoint() {
        JmsEndpoint jmsEndpoint = new JmsEndpoint(receiveAggregatedLogMessageEndpointUri,
            activeMQComponent, receiveAggregatedLogMessageEndpointUri, true, jmsConfiguration);
        return  jmsEndpoint;
    }
/*
    @Bean
    public JmsConsumer jmsConsumer() {
        JmsConsumer jmsConsumer = new JmsConsumer();
            jmsConsumer.

        }
*/
    @Override

    public CamelContext camelContext() throws Exception {
        CamelContext camelContext = new SpringCamelContext();
        camelContext.addEndpoint(receiveLogMessageEndpointUri, receiveLogMessageEndpoint());
        camelContext.addEndpoint(receiveAggregatedLogMessageEndpointUri, receiveAggregatedLogMessageEndpoint());
        camelContext.addRoutes(logSenderRouteBuilder);
        camelContext.disableJMX();
        return camelContext;
    }
/*
    @Override
    public List<RouteBuilder> routes() {
        return Collections.singletonList(logSenderRouteBuilder);
    }
*/
}
