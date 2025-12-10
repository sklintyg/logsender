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

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;
import se.inera.intyg.logsender.config.LogSenderBeanConfig;
import se.inera.intyg.logsender.config.LogsenderProperties;
import se.inera.intyg.logsender.mocks.MockTransactionManager;
import se.inera.intyg.logsender.routes.LogSenderRouteBuilder;

@Lazy
@Configuration
@EnableConfigurationProperties(LogsenderProperties.class)
@Import({
    LogSenderBeanConfig.class,
    CamelAutoConfiguration.class,  // Camel autoconfiguration
    LogSenderRouteBuilder.class    // The route being tested
})
public class UnitTestConfig {

  @Bean
  public MockTransactionManager transactionManager() {
    return new MockTransactionManager();
  }

  @Bean
  public MockLogSenderClientImpl mockSendCertificateServiceClient() {
    return new MockLogSenderClientImpl();
  }
}
