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
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@ConfigurationProperties(prefix = "logsender")
@Validated
public class LogsenderProperties {

  private Aggregation aggregation = new Aggregation();

  private Queue queue = new Queue();

  private Loggtjanst loggtjanst = new Loggtjanst();

  private Certificate certificate = new Certificate();

  @Setter
  @Getter
  public static class Aggregation {

    @NotNull
    @Min(1)
    private Integer bulkSize;

    @NotNull
    @Min(1000)
    private Long bulkTimeout;

  }


  @Setter
  @Getter
  public static class Queue {

    @NotBlank
    private String receiveLogMessageEndpoint;

    @NotBlank
    private String receiveAggregatedLogMessageEndpoint;

    @NotBlank
    private String receiveAggregatedLogMessageDlq;

  }

  @Setter
  @Getter
  public static class Loggtjanst {

    @NotBlank
    private String logicalAddress;

    @NotBlank
    private String endpointUrl;

  }

  @Setter
  @Getter
  public static class Certificate {

    private String file;

    @NotBlank
    private String type;

    private String truststoreFile;

    @NotBlank
    private String truststoreType;

    private String password;

    private String keyManagerPassword;

    private String truststorePassword;

  }
}

