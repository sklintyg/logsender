/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import java.util.Collections;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import se.inera.intyg.logsender.loggtjanststub.LogStore;
import se.inera.intyg.logsender.loggtjanststub.LoggtjanstStubRestApi;
import se.inera.intyg.logsender.loggtjanststub.StoreLogStubResponder;
import se.inera.intyg.logsender.loggtjanststub.StubState;
import se.inera.intyg.logsender.loggtjanststub.json.LogStoreObjectMapper;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;


@Configuration
@Profile({"dev & !test", "wc-all-stubs & !test", "wc-loggtjanst-stub & !test"})
public class LoggtjanstStubConfig {

  @Bean
  public LogStore logStore() {
    return new LogStore();
  }

  @Bean
  public StubState stubState() {
    return new StubState();
  }

  @Bean
  public LogStoreObjectMapper logStoreObjectMapper() {
    return new LogStoreObjectMapper();
  }

  @Bean
  public StoreLogResponderInterface storeLogStubResponder(LogStore logStore, StubState stubState) {
    return new StoreLogStubResponder(logStore, stubState);
  }

  @Bean
  public EndpointImpl storeLogEndpoint(Bus cxfBus,
      StoreLogResponderInterface storeLogStubResponder) {
    EndpointImpl endpoint = new EndpointImpl(cxfBus, storeLogStubResponder);
    endpoint.publish("/stubs/informationsecurity/auditing/log/StoreLog/v2/rivtabp21");
    return endpoint;
  }

  @Configuration
  @Profile({"dev", "testability-api"})
  public static class TestabilityApiConfig {

    @Bean
    public LoggtjanstStubRestApi loggtjanstStubRestApi() {
      return new LoggtjanstStubRestApi();
    }

    @Bean
    public JacksonJsonProvider jacksonJsonProvider(LogStoreObjectMapper logStoreObjectMapper) {
      JacksonJsonProvider provider = new JacksonJsonProvider();
      provider.setMapper(logStoreObjectMapper);
      return provider;
    }

    @Bean
    public JAXRSServerFactoryBean loggtjanstApiServer(Bus cxfBus,
        LoggtjanstStubRestApi loggtjanstStubRestApi,
        JacksonJsonProvider jacksonJsonProvider) {
      JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
      factory.setBus(cxfBus);
      factory.setAddress("/api/loggtjanst-api");
      factory.setServiceBeans(Collections.singletonList(loggtjanstStubRestApi));
      factory.setProviders(Collections.singletonList(jacksonJsonProvider));
      factory.setExtensionMappings(Collections.singletonMap("json", "application/json"));
      factory.create();
      return factory;
    }
  }
}

