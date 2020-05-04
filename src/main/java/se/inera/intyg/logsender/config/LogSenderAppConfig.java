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

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource("file:${config.file}")
@PropertySource("file:${credentials.file}")
@Import({LogSenderBeanConfig.class, LogSenderJmsConfig.class, LogSenderCamelConfig.class, LogSenderWsConfig.class})
@ImportResource(locations = {"classpath:basic-cache-config.xml", "classpath:/loggtjanst-stub-context.xml"})
public class LogSenderAppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setBasenames("version");
        return messageSource;
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public Bus cxf() {
        return new SpringBus();
    }
}
