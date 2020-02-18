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
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource("file:${config.file}")
@PropertySource("file:${credentials.file}")
public class WsTlsConfig {
/*
    @Value("${sakerhetstjanst.ws.certificate.type}")
    private String keyStoreType;

    @Value("${sakerhetstjanst.ws.truststore.type}")
    private String trustStoreType;

    //@Value(value = "${sakerhetstjanst.ws.certificate.file}")
    private String keyStoreFile;

    //@Value("${sakerhetstjanst.ws.truststore.file}")
    private String trustStoreFile;

    @Value("${sakerhetstjanst.ws.certificate.password}")
    private char[] keyStorePassword;

    //@Value("${sakerhetstjanst.ws.truststore.password}")
    private char[] trustStorePassword;

    private final QName serviceName = QName.valueOf(
        "{urn:riv:informationsecurity:auditing:log:StoreLog:2:rivtabp21}StoreLogResponderInterfacePort.http-conduit");

    @Bean
    public void configureTlsParameters(StoreLogResponderInterface port) throws UnrecoverableKeyException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, IOException {
        Client client = ClientProxy.getClient(port);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        //HttpConduitConfig config = new HttpConduitConfig();
        //config.setClientPolicy(setupHTTPClientPolicy());
        //config.setTlsClientParameters(setupTLSClientParameters());
        config().apply(httpConduit);
    }

    @Bean
    @Profile("!dev")
    public HttpConduitConfig getHttpConduitConfig() throws UnrecoverableKeyException, CertificateException,
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
        tlsClientParameters.setKeyManagers(setupKeyManagers());
        tlsClientParameters.setTrustManagers(setupTrustManagers());
        tlsClientParameters.setCipherSuitesFilter(setupCipherSuitesFilter());
        return tlsClientParameters;
    }

    private KeyManager[] setupKeyManagers() throws KeyStoreException, IOException, UnrecoverableKeyException,
        NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyStoreType);
        keyManagerFactory.init(keyStore, keyStorePassword);
        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] setupTrustManagers() throws KeyStoreException, IOException, CertificateException,
        NoSuchAlgorithmException {
        final KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        trustStore.load(new FileInputStream(trustStoreFile), trustStorePassword);
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

 */
}
