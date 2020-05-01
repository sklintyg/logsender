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

import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.support.TransactionTemplate;
import se.inera.intyg.logsender.client.mock.MockLogSenderClientImpl;
import se.inera.intyg.logsender.config.LogSenderAppConfig;
import se.inera.intyg.logsender.config.LogSenderBeanConfig;
import se.inera.intyg.logsender.config.LogSenderCamelConfig;
import se.inera.intyg.logsender.config.LogSenderJmsConfig;
import se.inera.intyg.logsender.config.LogSenderWsConfig;
import se.inera.intyg.logsender.mocks.MockTransactionManager;
import se.inera.intyg.logsender.routes.LogSenderRouteBuilder;


@Lazy
@Configuration
//@Import(LogSenderAppConfig.class)

//@PropertySource("classpath:logsender/unit-test.properties")
//@TestPropertySource("classpath:logsender/unit-test.properties")
//@Import({LogSenderBeanConfig.class, LogSenderCamelConfig.class, LogSenderRouteBuilder.class})
//@Import({LogSenderBeanConfig.class, LogSenderCamelConfig.class})
//@ImportResource({"classpath:/basic-cache-config.xml", "classpath:/loggtjanst-stub-context.xml"})
@ComponentScan(basePackages = "se.inera.intyg.logsender")
public class UnitTestConfig {

    @Bean
    public MockTransactionManager transactionManager() {
        return new MockTransactionManager();
    }

    @Primary
    @Bean
    public SpringTransactionPolicy policy() {
        SpringTransactionPolicy policy = new SpringTransactionPolicy(transactionManager());
        return policy;
    }
}

