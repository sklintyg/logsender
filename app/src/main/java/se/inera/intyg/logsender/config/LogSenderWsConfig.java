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

import jakarta.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import lombok.RequiredArgsConstructor;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.spring.JaxWsProxyFactoryBeanDefinitionParser.JAXWSSpringClientProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;

@Configuration
@RequiredArgsConstructor
public class LogSenderWsConfig {

  private static final int LOG_MESSAGE_SIZE = 1024;
  private final LogsenderProperties properties;

  @Resource
  private Environment env;

  @Bean
  @Profile("!testability")
  @SchemaValidation(type = SchemaValidationType.BOTH)
  public StoreLogResponderInterface storeLogClient() throws UnrecoverableKeyException,
      CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
    final var jaxWsProxyFactoryBean = createJaxWsProxyFactoryBean();
    final var storeLogClient = (StoreLogResponderInterface) jaxWsProxyFactoryBean.create();
    setClient(storeLogClient);
    return storeLogClient;
  }

  private JaxWsProxyFactoryBean createJaxWsProxyFactoryBean() {
    final var jaxWsProxyFactoryBean = new JAXWSSpringClientProxyFactoryBean();
    jaxWsProxyFactoryBean.setServiceClass(StoreLogResponderInterface.class);
    jaxWsProxyFactoryBean.setAddress(properties.storeLog().endpointUrl());
    jaxWsProxyFactoryBean.getFeatures().add(loggingFeature());
    return jaxWsProxyFactoryBean;
  }

  private LoggingFeature loggingFeature() {
    final var loggingFeature = new LoggingFeature();
    loggingFeature.setLimit(LOG_MESSAGE_SIZE * LOG_MESSAGE_SIZE);
    loggingFeature.setPrettyLogging(true);
    return loggingFeature;
  }

  private void setClient(StoreLogResponderInterface storeLogClient)
      throws UnrecoverableKeyException,
      CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
    final var client = ClientProxy.getClient(storeLogClient);
    if (!Arrays.asList(this.env.getActiveProfiles()).contains("dev")) {
      final var httpConduit = (HTTPConduit) client.getConduit();
      configureTlsParameters().apply(httpConduit);
    }
  }

  private HttpConduitConfig configureTlsParameters()
      throws UnrecoverableKeyException, CertificateException,
      NoSuchAlgorithmException, KeyStoreException, IOException {
    final var config = new HttpConduitConfig();
    config.setClientPolicy(setupHTTPClientPolicy());
    config.setTlsClientParameters(setupTLSClientParameters());
    return config;
  }

  private HTTPClientPolicy setupHTTPClientPolicy() {
    final var httpClientPolicy = new HTTPClientPolicy();
    httpClientPolicy.setAllowChunking(false);
    httpClientPolicy.setAutoRedirect(true);
    httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
    return httpClientPolicy;
  }

  private TLSClientParameters setupTLSClientParameters() throws KeyStoreException, IOException,
      NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
    final var tlsClientParameters = new TLSClientParameters();
    tlsClientParameters.setDisableCNCheck(true);
    tlsClientParameters.setCipherSuitesFilter(setupCipherSuitesFilter());
    tlsClientParameters.setKeyManagers(setupKeyManagers());
    tlsClientParameters.setTrustManagers(setupTrustManagers());
    return tlsClientParameters;
  }

  private KeyManager[] setupKeyManagers()
      throws KeyStoreException, IOException, UnrecoverableKeyException,
      NoSuchAlgorithmException, CertificateException {
    final var keyStoreFile = properties.storeLog().certificate().file();
    final var keyStorePassword = properties.storeLog().certificate().password().toCharArray();
    final var keyStore = KeyStore.getInstance(properties.storeLog().certificate().type());
    try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
      keyStore.load(keyStoreInputStream, keyStorePassword);
    }
    final var keyManagerFactory = KeyManagerFactory.getInstance(
        KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, keyStorePassword);
    return keyManagerFactory.getKeyManagers();
  }

  private TrustManager[] setupTrustManagers()
      throws KeyStoreException, IOException, CertificateException,
      NoSuchAlgorithmException {
    final var trustStoreFile = properties.storeLog().trustStore().file();
    final var trustStorePassword = properties.storeLog().trustStore().password().toCharArray();
    final var trustStore = KeyStore.getInstance(properties.storeLog().trustStore().type());
    try (FileInputStream trustStoreInputStream = new FileInputStream(trustStoreFile)) {
      trustStore.load(trustStoreInputStream, trustStorePassword);
    }
    final var trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    return trustManagerFactory.getTrustManagers();
  }

  private FiltersType setupCipherSuitesFilter() {
    final var cipherSuitesFilter = new FiltersType();
    cipherSuitesFilter.getInclude().add(".*_EXPORT_.*");
    cipherSuitesFilter.getInclude().add(".*_EXPORT1024_.*");
    cipherSuitesFilter.getInclude().add(".*_WITH_AES_256_.*");
    cipherSuitesFilter.getInclude().add(".*_WITH_AES_128_.*");
    cipherSuitesFilter.getInclude().add(".*_WITH_3DES_.*");
    cipherSuitesFilter.getInclude().add(".*_WITH_DES_.*");
    cipherSuitesFilter.getInclude().add(".*_WITH_NULL_.*");
    cipherSuitesFilter.getExclude().add(".*_DH_anon_.*");
    return cipherSuitesFilter;
  }
}
