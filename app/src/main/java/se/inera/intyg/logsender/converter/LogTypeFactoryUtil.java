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
package se.inera.intyg.logsender.converter;

import java.util.Optional;
import se.inera.intyg.schemas.contract.Personnummer;

final class LogTypeFactoryUtil {

  private static final String KODVERK_SAMORDNINGSNUMMER = "1.2.752.129.2.1.3.3";
  private static final String KODVERK_PERSONNUMMER = "1.2.752.129.2.1.3.1";

  private static final int SAMORDNING_MONTH_INDEX = 6;
  private static final int SAMORDNING_MONTH_VALUE_MIN = 6;

  private static final LogTypeFactoryUtil instance = new LogTypeFactoryUtil();

  private LogTypeFactoryUtil() {
  }

  static LogTypeFactoryUtil getInstance() {
    return instance;
  }


  public boolean isSamordningsNummer(Personnummer personNummer) {

    if (Optional.ofNullable(personNummer).isPresent()) {
      String normalizedPersonnummer = personNummer.getPersonnummer();
      char dateDigit = normalizedPersonnummer.charAt(SAMORDNING_MONTH_INDEX);
      return Character.getNumericValue(dateDigit) >= SAMORDNING_MONTH_VALUE_MIN;
    }

    return false;
  }

  public String getSamordningsNummerRoot() {
    return KODVERK_SAMORDNINGSNUMMER;
  }

  public String getPersonnummerRoot() {
    return KODVERK_PERSONNUMMER;
  }

  public String trim(String input) {
    return input != null ? input.trim() : null;
  }


  public String trimToNull(String input) {
    return input != null && !input.trim().isEmpty() ? input.trim() : null;
  }

}
