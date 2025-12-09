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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for logsender application.
 * Replaces multiple @Value annotations with centralized, validated configuration.
 *
 * Properties are loaded from application.properties with prefix "logsender".
 */
@ConfigurationProperties(prefix = "logsender")
@Validated
public class LogsenderProperties {

    /**
     * Message aggregation configuration.
     */
    private Aggregation aggregation = new Aggregation();

    /**
     * JMS queue configuration.
     */
    private Queue queue = new Queue();

    /**
     * Loggtjänst endpoint configuration.
     */
    private Loggtjanst loggtjanst = new Loggtjanst();

    /**
     * Certificate configuration for SOAP communication.
     */
    private Certificate certificate = new Certificate();

    public Aggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public Loggtjanst getLoggtjanst() {
        return loggtjanst;
    }

    public void setLoggtjanst(Loggtjanst loggtjanst) {
        this.loggtjanst = loggtjanst;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * Message aggregation settings.
     */
    public static class Aggregation {

        /**
         * Number of messages to aggregate into a single batch.
         */
        @NotNull
        @Min(1)
        private Integer bulkSize;

        /**
         * Maximum time in milliseconds to wait before sending a batch.
         */
        @NotNull
        @Min(1000)
        private Long bulkTimeout;

        public Integer getBulkSize() {
            return bulkSize;
        }

        public void setBulkSize(Integer bulkSize) {
            this.bulkSize = bulkSize;
        }

        public Long getBulkTimeout() {
            return bulkTimeout;
        }

        public void setBulkTimeout(Long bulkTimeout) {
            this.bulkTimeout = bulkTimeout;
        }
    }

    /**
     * JMS queue endpoint URIs.
     */
    public static class Queue {

        /**
         * Queue URI for receiving individual log messages.
         */
        @NotBlank
        private String receiveLogMessageEndpoint;

        /**
         * Queue URI for receiving aggregated log messages.
         */
        @NotBlank
        private String receiveAggregatedLogMessageEndpoint;

        /**
         * Dead letter queue URI for failed aggregated messages.
         */
        @NotBlank
        private String receiveAggregatedLogMessageDlq;

        public String getReceiveLogMessageEndpoint() {
            return receiveLogMessageEndpoint;
        }

        public void setReceiveLogMessageEndpoint(String receiveLogMessageEndpoint) {
            this.receiveLogMessageEndpoint = receiveLogMessageEndpoint;
        }

        public String getReceiveAggregatedLogMessageEndpoint() {
            return receiveAggregatedLogMessageEndpoint;
        }

        public void setReceiveAggregatedLogMessageEndpoint(String receiveAggregatedLogMessageEndpoint) {
            this.receiveAggregatedLogMessageEndpoint = receiveAggregatedLogMessageEndpoint;
        }

        public String getReceiveAggregatedLogMessageDlq() {
            return receiveAggregatedLogMessageDlq;
        }

        public void setReceiveAggregatedLogMessageDlq(String receiveAggregatedLogMessageDlq) {
            this.receiveAggregatedLogMessageDlq = receiveAggregatedLogMessageDlq;
        }
    }

    /**
     * Loggtjänst SOAP service configuration.
     */
    public static class Loggtjanst {

        /**
         * Logical address for the logging service.
         */
        @NotBlank
        private String logicalAddress;

        /**
         * SOAP endpoint URL for the logging service.
         */
        @NotBlank
        private String endpointUrl;

        public String getLogicalAddress() {
            return logicalAddress;
        }

        public void setLogicalAddress(String logicalAddress) {
            this.logicalAddress = logicalAddress;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }
    }

    /**
     * Certificate and truststore configuration for secure SOAP communication.
     */
    public static class Certificate {

        /**
         * Path to the certificate keystore file.
         */
        private String file;

        /**
         * Type of the keystore (JKS, PKCS12, etc).
         */
        @NotBlank
        private String type;

        /**
         * Path to the truststore file.
         */
        private String truststoreFile;

        /**
         * Type of the truststore.
         */
        @NotBlank
        private String truststoreType;

        /**
         * Password for the certificate keystore.
         */
        private String password;

        /**
         * Password for the key manager.
         */
        private String keyManagerPassword;

        /**
         * Password for the truststore.
         */
        private String truststorePassword;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTruststoreFile() {
            return truststoreFile;
        }

        public void setTruststoreFile(String truststoreFile) {
            this.truststoreFile = truststoreFile;
        }

        public String getTruststoreType() {
            return truststoreType;
        }

        public void setTruststoreType(String truststoreType) {
            this.truststoreType = truststoreType;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getKeyManagerPassword() {
            return keyManagerPassword;
        }

        public void setKeyManagerPassword(String keyManagerPassword) {
            this.keyManagerPassword = keyManagerPassword;
        }

        public String getTruststorePassword() {
            return truststorePassword;
        }

        public void setTruststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
        }
    }
}

