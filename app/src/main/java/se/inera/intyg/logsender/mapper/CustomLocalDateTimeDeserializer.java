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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    private static final int MILLI_TO_NANO = 1_000_000;
    private static final int MAX_MILLIS = 1_000;
    private static final int LOCAL_DATE_LENGTH = 10;
    private static final long serialVersionUID = 1L;

    public CustomLocalDateTimeDeserializer() {
        this(null);
    }

    public CustomLocalDateTimeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        if (parser.hasTokenId(JsonTokenId.ID_STRING)) {
            String string = parser.getText().trim();
            if (string.isEmpty()) {
                return null;
            }

            try {
                if (string.length() > LOCAL_DATE_LENGTH) {
                    if (string.charAt(LOCAL_DATE_LENGTH) == 'T') {
                        if (string.endsWith("Z")) {
                            return LocalDateTime.ofInstant(Instant.parse(string), ZoneOffset.UTC);
                        } else {
                            return LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                    } else {
                        throw JsonMappingException.from(parser, String.format("Failed to deserialize %s as LocalDateTime", string));
                    }
                } else {
                    return LocalDate.parse(string, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                }

            } catch (DateTimeException e) {
                rethrowDateTimeException(parser, context, e, string);
            }
        }
        if (parser.isExpectedStartArrayToken()) {
            if (parser.nextToken() == JsonToken.END_ARRAY) {
                return null;
            }
            int year = parser.getIntValue();
            int month = parser.nextIntValue(-1);
            int day = parser.nextIntValue(-1);
            int hour = parser.nextIntValue(-1);
            int minute = parser.nextIntValue(-1);

            if (parser.nextToken() != JsonToken.END_ARRAY) {
                int second = parser.getIntValue();

                if (parser.nextToken() != JsonToken.END_ARRAY) {
                    int partialSecond = parser.getIntValue();
                    if (partialSecond < MAX_MILLIS && !context.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
                        partialSecond *= MILLI_TO_NANO; // value is milliseconds, convert it to nanoseconds
                    }

                    if (parser.nextToken() != JsonToken.END_ARRAY) {
                        throw context.wrongTokenException(parser, LocalDateTime.class, JsonToken.END_ARRAY, "Expected array to end.");
                    }
                    return LocalDateTime.of(year, month, day, hour, minute, second, partialSecond);
                }
                return LocalDateTime.of(year, month, day, hour, minute, second);
            }
            return LocalDateTime.of(year, month, day, hour, minute);
        }
        if (parser.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (LocalDateTime) parser.getEmbeddedObject();
        }
        throw context.wrongTokenException(parser, LocalDateTime.class, JsonToken.VALUE_STRING, "Expected array or string.");
    }

    private void rethrowDateTimeException(JsonParser p, DeserializationContext context,
        DateTimeException e0, String value) throws JsonMappingException {
        JsonMappingException e;
        if (e0 instanceof DateTimeParseException) {
            e = context.weirdStringException(value, handledType(), e0.getMessage());
            e.initCause(e0);
        } else {
            e = JsonMappingException.from(p,
                String.format("Failed to deserialize %s: (%s) %s", handledType().getName(), e0.getClass().getName(), e0.getMessage()),
                e0);
        }
        throw e;
    }
}
