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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record LogsenderProperties(
    @NotNull @Valid Aggregation aggregation,
    @NotNull @Valid Queue queue,
    @NotNull @Valid StoreLog storeLog
) {

  public record Aggregation(
      @NotNull @Min(1) @Valid Integer bulkSize,
      @NotNull @Min(1000) @Valid Long bulkTimeout
  ) {

  }

  @Validated
  public record Queue(
      @NotBlank @Valid String receiveLogMessageEndpoint,
      @NotBlank @Valid String receiveAggregatedLogMessageEndpoint,
      @NotBlank @Valid String receiveAggregatedLogMessageDlq
  ) {

  }

  public record StoreLog(
      @NotBlank @Valid String logicalAddress,
      @NotBlank @Valid String endpointUrl,
      @NotBlank @Valid String ntjpBaseUrl,
      @NotNull @Valid Certificate certificate,
      @NotNull @Valid TrustStore trustStore
  ) {

  }

  public record Certificate(
      @NotBlank @Valid String file,
      @NotBlank @Valid String type,
      @NotBlank @Valid String password,
      @NotBlank @Valid String keyManagerPassword
  ) {

  }

  public record TrustStore(
      @NotBlank @Valid String file,
      @NotBlank @Valid String type,
      @NotBlank @Valid String password
  ) {

  }
}

