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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.inera.intyg.logsender.client.LogSenderClient;
import se.inera.intyg.logsender.client.LogSenderClientImpl;
import se.inera.intyg.logsender.converter.LogTypeFactory;
import se.inera.intyg.logsender.converter.LogTypeFactoryImpl;
import se.inera.intyg.logsender.routes.LogSenderRouteBuilder;
import se.inera.intyg.logsender.service.LogMessageAggregationProcessor;
import se.inera.intyg.logsender.service.LogMessageSendProcessor;
import se.inera.intyg.logsender.service.LogMessageSplitProcessor;
import se.inera.intyg.logsender.service.SoapIntegrationService;
import se.inera.intyg.logsender.service.SoapIntegrationServiceImpl;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;

@Configuration
public class LogSenderBeanConfig {

    @Bean
    public LogSenderClient logSenderClient(SoapIntegrationService soapIntegrationService) {
        return new LogSenderClientImpl(soapIntegrationService);
    }

    @Bean
    public LogTypeFactory logTypeFactory() {
        return new LogTypeFactoryImpl();
    }

    @Bean
    public LogMessageSendProcessor logMessageSendProcessor() {
        return new LogMessageSendProcessor();
    }

    @Bean
    public LogMessageAggregationProcessor logMessageAggregationProcessor() {
        return new LogMessageAggregationProcessor();
    }

    @Bean
    public LogMessageSplitProcessor logMessageSplitProcessor() {
        return new LogMessageSplitProcessor();
    }

    @Bean
    public LogSenderRouteBuilder logSenderRouteBuilder() {
        return new LogSenderRouteBuilder();
    }

    @Bean
    public SoapIntegrationService soapIntegrationService(StoreLogResponderInterface storeLogResponderInterface) {
        return new SoapIntegrationServiceImpl(storeLogResponderInterface);
    }
}
