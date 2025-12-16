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

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class InternalDateAdapter {

    private InternalDateAdapter() {
    }

    /**
     * Print the string wrapped by the InternalDate object.
     *
     * @param date the InternalDate-object
     * @return the string wrapped
     */
    public static String printInternalDate(InternalDate date) {

        if (date == null) {
            return null;
        }

        return date.getDate();
    }

    /**
     * Attempt to parse a string to a LocalDate wrapped by an InternalDate, attempts several parsing strategies on the
     * string, if none are successful the bare string will be wrapped allowing for non-valid dates to be saved as well.
     *
     * @return InternalDate wrapping either a valid LocalDate, or an arbitrary string
     */
    public static InternalDate parseInternalDate(String string) {
        LocalDate localDate = getLocalDate(string);
        if (localDate == null) {
            return new InternalDate(string);
        } else {
            return new InternalDate(localDate);
        }
    }

    /**
     * Make an InternalDate from int values, ensures that two digits are used in all positions (i.e '09' not '9').
     *
     * @return InternalDate
     */
    public static InternalDate parseInternalDate(int year, int month, int day) {
        // Build a nice datestring adding 0 to single digits etc.
        DecimalFormat df = new DecimalFormat("00");
        String dateString = String.format("%d-%s-%s", year, df.format(month), df.format(day));
        return new InternalDate(dateString);
    }

    //CHECKSTYLE:OFF EmptyCatchBlock
    private static LocalDate getLocalDate(String str) {
        try {
            return LocalDate.parse(str, DateTimeFormatter.ISO_DATE);

        } catch (Exception e) {
        }
        try {
            return LocalDate.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
        }
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } catch (Exception e) {
            return null;
        }
    }
    //CHECKSTYLE:ON EmptyCatchBlock

}
