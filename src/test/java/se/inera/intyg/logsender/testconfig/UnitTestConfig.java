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

package se.inera.intyg.logsender.testconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;
import se.inera.intyg.logsender.config.LogSenderBeanConfig;
import se.inera.intyg.logsender.config.LogSenderCamelConfig;
import se.inera.intyg.logsender.mocks.MockTransactionManager;

@Lazy
@Configuration
@Import({LogSenderBeanConfig.class, LogSenderCamelConfig.class})
@PropertySource({"classpath:application.properties"})
@ImportResource({"classpath:/basic-cache-config.xml", "classpath:/loggtjanst-stub-context.xml"})
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

