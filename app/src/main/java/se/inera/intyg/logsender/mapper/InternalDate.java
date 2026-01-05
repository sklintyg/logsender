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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import se.inera.intyg.logsender.exception.ModelException;

@Setter
@Getter
public class InternalDate {

  private static final DateTimeFormatter PARSER = DateTimeFormatter.ISO_DATE;

  /*
   * Getters and setters
   */
  private String date;

  /**
   * Default constructor.
   */
  public InternalDate() {
    // Needed for deserialization
  }

  /**
   * Constuct an {@link InternalDate} from a String.
   *
   * @param date a String
   */
  public InternalDate(String date) {
    this.date = date;
  }

  /**
   * Constuct an {@link InternalDate} from a {@link LocalDate}, primarily used when converting from
   * external to internal model.
   *
   * @param date a {@link LocalDate}
   */
  public InternalDate(LocalDate date) {
    if (date == null) {
      throw new ModelException("Got null while creating date object");
    }
    this.date = date.format(PARSER);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!(object instanceof InternalDate that)) {
      return false;
    }
    return Objects.equals(this.date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.date);
  }

  @Override
  public String toString() {
    return Objects.toString(date);
  }
}
