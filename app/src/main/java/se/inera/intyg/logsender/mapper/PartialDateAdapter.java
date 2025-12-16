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
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

/**
 * @author andreaskaltenbach
 */
public final class PartialDateAdapter {

    private static final int YEAR_LENGTH = 4;
    private static final int YEAR_MONTH_LENGTH = 7;

    private PartialDateAdapter() {
    }

    public static Temporal parsePartialDate(String dateString) {
        if (dateString == null) {
            return null;
        }

        switch (dateString.length()) {
            case YEAR_LENGTH:
                return Year.parse(dateString);
            case YEAR_MONTH_LENGTH:
                return YearMonth.parse(dateString);
            default:
                return LocalDate.parse(dateString);
        }
    }

    public static String printPartialDate(Temporal temporal) {
        if (temporal == null) {
            return null;
        } else if (temporal.isSupported(ChronoField.DAY_OF_MONTH)) {
            return LocalDate.from(temporal).format(DateTimeFormatter.ISO_DATE);
        } else {
            return temporal.toString();
        }
    }
}
