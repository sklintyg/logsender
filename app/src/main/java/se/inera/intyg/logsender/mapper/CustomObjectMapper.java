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
package se.inera.intyg.logsender.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import org.springframework.stereotype.Component;

/**
 * Customized Jackson ObjectMapper for the inera-certificate projects.
 * <p>
 * -registers additional serializers and deserializers for date and time types -registers a
 * specialized serializer to represent certificate-specific data as JSON
 */

@Component("objectMapper")
public class CustomObjectMapper extends ObjectMapper {

  @Serial
  private static final long serialVersionUID = 1L;

  public CustomObjectMapper() {
    // NON_NULL indicates that only properties with non-null values are to be included.
    setSerializationInclusion(JsonInclude.Include.NON_NULL);
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    registerModule(new CustomModule());
  }

  private static final class CustomModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    private CustomModule() {
      addSerializer(Temporal.class, new TemporalSerializer());
      addDeserializer(Temporal.class, new TemporalDeserializer());

      addSerializer(InternalDate.class, new InternalDateSerializer());
      addDeserializer(InternalDate.class, new InternalDateDeserializer());

      addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
      addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());

      addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE);
      /*
       * Using a custom crafted deserializer that handles dates
       * on the UTC format. The original LocalDateDeserializer class do not
       * handle the UTC format.
       */
      addDeserializer(LocalDate.class, new CustomLocalDateDeserializer());
    }

  }
}
