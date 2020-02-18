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

import org.apache.camel.CamelContext;

import org.apache.camel.spring.CamelEndpointFactoryBean;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import se.inera.intyg.logsender.routes.LogSenderRouteBuilder;

@Configuration
@PropertySource("classpath:default.properties")
//@PropertySource(value = "file:${config.file}")
//@PropertySource("file:${credentials.file}")
public class CamelConfig extends CamelConfiguration {

    @Value("${receiveLogMessageEndpointUri}")
    String receiveLogMessageEndpointUri;

    @Value("${receiveAggregatedLogMessageEndpointUri}")
    String receiveAggregatedLogMessageEndpointUri;

    @Bean
    public LogSenderRouteBuilder logSenderRouteBuilder() {
        return new LogSenderRouteBuilder();
    }

    @Bean
    public CamelEndpointFactoryBean receiveLogMessageEndpoint() {
        CamelEndpointFactoryBean receiveLogMessageEndpoint = new CamelEndpointFactoryBean();
        receiveLogMessageEndpoint.setId("receiveAggregatedLogMessageEndpoint");
        receiveLogMessageEndpoint.setUri(receiveLogMessageEndpointUri);
        return receiveLogMessageEndpoint;
    }

    @Bean
    public CamelEndpointFactoryBean receiveAggregatedLogMessageEndpoint() {
        CamelEndpointFactoryBean receiveAggregatedLogMessageEndpoint = new CamelEndpointFactoryBean();
        receiveAggregatedLogMessageEndpoint.setId("receiveAggregatedLogMessageEndpoint");
        receiveAggregatedLogMessageEndpoint.setUri(receiveAggregatedLogMessageEndpointUri);
        return receiveAggregatedLogMessageEndpoint;
    }

    @Override
    public CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = new SpringCamelContext();
        camelContext.addRoutes(logSenderRouteBuilder());
        return camelContext;
    }
}
