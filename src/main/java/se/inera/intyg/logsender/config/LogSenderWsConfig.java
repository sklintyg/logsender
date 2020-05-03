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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Resource;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.spring.JaxWsProxyFactoryBeanDefinitionParser.JAXWSSpringClientProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;

@Configuration
public class LogSenderWsConfig {

    @Resource
    private Environment env;

    @Value("${sakerhetstjanst.ws.certificate.type}")
    private String keyStoreType;
    @Value("${sakerhetstjanst.ws.truststore.type}")
    private String trustStoreType;
    @Value("${loggtjanst.endpoint.url}")
    private String loggTjanstEndpointUrl;

    private static final int LOG_MESSAGE_SIZE = 1024;

    @Bean
    @SchemaValidation(type = SchemaValidationType.BOTH)
    public StoreLogResponderInterface storeLogClient() throws UnrecoverableKeyException,
        CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        env.getActiveProfiles();
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = createJaxWsProxyFactoryBean();
        StoreLogResponderInterface storeLogClient =
            (StoreLogResponderInterface) jaxWsProxyFactoryBean.create();
        setClient(storeLogClient);
        return storeLogClient;
    }

    private JaxWsProxyFactoryBean createJaxWsProxyFactoryBean() {
        JAXWSSpringClientProxyFactoryBean jaxWsProxyFactoryBean = new JAXWSSpringClientProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(StoreLogResponderInterface.class);
        jaxWsProxyFactoryBean.setAddress(loggTjanstEndpointUrl);
        jaxWsProxyFactoryBean.getFeatures().add(loggingFeature());
        return jaxWsProxyFactoryBean;
    }

    private LoggingFeature loggingFeature() {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setLimit(LOG_MESSAGE_SIZE * LOG_MESSAGE_SIZE);
        loggingFeature.setPrettyLogging(true);
        return loggingFeature;
    }

    private void setClient(StoreLogResponderInterface storeLogClient) throws UnrecoverableKeyException,
        CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        Client client = ClientProxy.getClient(storeLogClient);
        if (!Arrays.asList(this.env.getActiveProfiles()).contains("dev")) {
            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
            configureTlsParameters().apply(httpConduit);
        }
    }

    private HttpConduitConfig configureTlsParameters() throws UnrecoverableKeyException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpConduitConfig config = new HttpConduitConfig();
        config.setClientPolicy(setupHTTPClientPolicy());
        config.setTlsClientParameters(setupTLSClientParameters());
        return config;
    }

    private HTTPClientPolicy setupHTTPClientPolicy() {
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setAutoRedirect(true);
        httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
        return httpClientPolicy;
    }

    private TLSClientParameters setupTLSClientParameters() throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setDisableCNCheck(true);
        tlsClientParameters.setCipherSuitesFilter(setupCipherSuitesFilter());
        if (!Arrays.asList(this.env.getActiveProfiles()).contains("dev")) {
            tlsClientParameters.setKeyManagers(setupKeyManagers());
            tlsClientParameters.setTrustManagers(setupTrustManagers());
        }
        return tlsClientParameters;
    }

    private KeyManager[] setupKeyManagers() throws KeyStoreException, IOException, UnrecoverableKeyException,
        NoSuchAlgorithmException, CertificateException {
        final String keyStoreFile = Objects.requireNonNull(env.getProperty("sakerhetstjanst.ws.certificate.file"));
        final char[] keyStorePassword = Objects.requireNonNull(env.getProperty("sakerhetstjanst.ws.certificate.password")).toCharArray();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
            keyStore.load(keyStoreInputStream, keyStorePassword);
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyStoreType);
        keyManagerFactory.init(keyStore, keyStorePassword);
        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] setupTrustManagers() throws KeyStoreException, IOException, CertificateException,
        NoSuchAlgorithmException {
        final String trustStoreFile = Objects.requireNonNull(env.getProperty("sakerhetstjanst.ws.truststore.file"));
        final char[] trustStorePassword =
            Objects.requireNonNull(env.getProperty("sakerhetstjanst.ws.truststore.password")).toCharArray();
        final KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        try (FileInputStream trustStoreInputStream = new FileInputStream(trustStoreFile)) {
            trustStore.load(trustStoreInputStream, trustStorePassword);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustStoreType);
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    private FiltersType setupCipherSuitesFilter() {
        FiltersType cipherSuitesFilter = new FiltersType();
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
