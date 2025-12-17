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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This class is based on the LocalDateDeserializer class. It's content was copied and slightly
 * changed to support dates on the UTC format.
 * <p>
 * Changes:
 * <li>
 * <ul>
 * The ISODateTimeFormat.dateTimeParser() is used instead of the ISODateTimeFormat.localDateParser()
 * </ul>
 * <ul>
 * In method deserializer the case START_ARRAY handles year, month and day as the original method does but here we do
 * not check if, after day, the next token is END_ARRAY since it's possible next token can be an int value (hours).
 * </ul>
 * </li>
 *
 * @author Magnus Ekstrand
 */
public class CustomLocalDateDeserializer extends StdDeserializer<LocalDate> {

  @Serial
  private static final long serialVersionUID = 1L;

  public CustomLocalDateDeserializer() {
    super(LocalDate.class);
  }

  /**
   * <strong>Description copied from class: JsonDeserializer</strong>
   * Method that can be called to ask implementation to deserialize json content into the value type
   * this serializer handles. Returned instance is to be constructed by method itself.
   * <p>
   * Pre-condition for this method is that the parser points to the first event that is part of
   * value to deserializer (and which is never Json 'null' literal, more on this below): for simple
   * types it may be the only value; and for structured types the Object start marker.
   * <p>
   * Post-condition is that the parser will point to the last event that is part of deserialized
   * value (or in case deserialization fails, event that was not recognized or usable, which may be
   * the same event as the one it pointed to upon call).
   * <p>
   * Note that this method is never called for JSON null literal, and thus deserializers need (and
   * should) not check for it.
   *
   * @param jp   - Parser used for reading Json content
   * @param ctxt - Context that can be used to access information about this deserialization
   *             activity.
   * @return Deserializer value as LocalDate
   */
  @Override
  public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {

    switch (jp.getCurrentToken()) {
      case START_ARRAY:
        // [yyyy,MM,dd]
        jp.nextToken(); // VALUE_NUMBER_INT
        int year = jp.getIntValue();
        jp.nextToken(); // VALUE_NUMBER_INT
        int month = jp.getIntValue();
        jp.nextToken(); // VALUE_NUMBER_INT
        int day = jp.getIntValue();
        jp.nextToken(); //END_ARRAY

        // We are only interested in year, month and day
        // Skip the time and return at date
        return LocalDate.of(year, month, day);
      case VALUE_NUMBER_INT:
        return Instant.ofEpochMilli(jp.getLongValue()).atZone(ZoneId.systemDefault()).toLocalDate();
      case VALUE_STRING:
        String str = jp.getText().trim();
        if (str.isEmpty()) { // [JACKSON-360]
          return null;
        }
        return LocalDate.parse(str,
            str.contains("T") ? DateTimeFormatter.ISO_DATE_TIME : DateTimeFormatter.ISO_DATE);
      default:
    }

    throw ctxt.wrongTokenException(jp, LocalDate.class, JsonToken.START_ARRAY,
        "expected JSON Array, Number or String");
  }
}
