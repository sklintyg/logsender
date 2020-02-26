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

import org.apache.camel.CamelContext;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import se.inera.intyg.logsender.config.LogSenderAppConfig;
import se.inera.intyg.logsender.config.LogSenderBeanConfig;
import se.inera.intyg.logsender.config.LogSenderCamelConfig;
import se.inera.intyg.logsender.mocks.MockTransactionManager;

@Configuration
@TestPropertySource("classpath:logsender/unit-test.properties")
@ContextConfiguration(classes = LogSenderAppConfig.class)
@Import(value = {LogSenderAppConfig.class})
public class UnitTestConfig {
/*
    @Autowired
    LogSenderAppConfig logSenderAppConfig;
*/
    @Bean
    public MockTransactionManager transactionManager() {
        return new MockTransactionManager();
    }


}
