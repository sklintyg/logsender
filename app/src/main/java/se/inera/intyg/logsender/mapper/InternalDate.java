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
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.inera.intyg.logsender.exception.ModelException;

public class InternalDate {

    private static final InternalDate MIN_DATE = new InternalDate("1900-01-01");
    private static final InternalDate MAX_DATE = new InternalDate("2099-12-12");
    private static final String DATE_FORMAT = "[1-2][0-9]{3,3}(-((0[1-9])|(1[0-2]))(-((0[1-9])|([1-2][0-9])|(3[0-1]))))";
    private static final String GENERAL_DATE_FORMAT = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
    private static final String GENERAL_DATE_FORMAT_YEAR = "([0-9]{4})-.*";
    private static final String GENERAL_DATE_FORMAT_MONTH = ".*-([0-9]{2})-.*";
    private static final DateTimeFormatter PARSER = DateTimeFormatter.ISO_DATE;

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
     * Constuct an {@link InternalDate} from a {@link LocalDate},
     * primarily used when converting from external to internal model.
     *
     * @param date a {@link LocalDate}
     */
    public InternalDate(LocalDate date) {
        if (date == null) {
            throw new ModelException("Got null while creating date object");
        }
        this.date = date.format(PARSER);
    }

    /*
     * Getters and setters
     */
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Attempts to parse the String held to a LocalDate.
     *
     * @return {@link LocalDate} if parsing was successful
     * @throws ModelException if parsing failed
     */
    public LocalDate asLocalDate() {
        if (date == null) {
            throw new ModelException("Date was null");
        }
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date, PARSER);
        } catch (DateTimeParseException ie) {
            throw new ModelException(String.format("Could not parse %s to LocalDate, failed with message: %s", date, ie.getMessage()));
        }
        return localDate;
    }

    /**
     * Check if the string held in this InternalDate matches expected dateformat (yyyy-MM-dd).
     *
     * @return true if it does, false otherwise
     */
    public boolean isValidDate() {
        if (date == null) {
            return false;
        }
        try {
            LocalDate.parse(date, PARSER);
        } catch (DateTimeParseException e) {
            return false;
        }
        return date.matches(DATE_FORMAT);
    }

    public boolean isCorrectFormat() {
        return date.matches(GENERAL_DATE_FORMAT);
    }

    public boolean isYearCorrectFormat() {
        return date.matches(GENERAL_DATE_FORMAT_YEAR);
    }

    public boolean isMonthCorrectFormat() {
        return date.matches(GENERAL_DATE_FORMAT_MONTH);
    }

    public boolean vagueDateInFuture() {
        LocalDate now = LocalDate.now();
        return vagueDateAfterDate(now);
    }

    public boolean vagueDateAfterDate(LocalDate dateLimit) {

        // This needs to be done on the string level since date parts can be 00, not parsable by LocalDate.parse
        Pattern yearPattern = Pattern.compile(GENERAL_DATE_FORMAT_YEAR);
        Matcher yearMatcher = yearPattern.matcher(date);
        if (yearMatcher.matches()) {
            if (yearMatcher.group(1).compareTo(Integer.toString(dateLimit.getYear())) > 0) {
                return true;
            }

            Pattern monthPattern = Pattern.compile(GENERAL_DATE_FORMAT_MONTH);
            Matcher monthMatcher = monthPattern.matcher(date);
            if (monthMatcher.matches()) {
                if (yearMatcher.group(1).equals(Integer.toString(dateLimit.getYear()))
                    && (Integer.parseInt(monthMatcher.group(1)) - dateLimit.getMonth().getValue()) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine whether an InternalDate is outside the allowed interval minDate < theDate < future.
     *
     * @return True if it is outside the allowed interval or is null or invalid, false otherwise.
     */
    public boolean beforeMinDateOrInFuture(LocalDate minDate) {
        if (date == null) {
            return true;
        }
        if (!this.isValidDate()) {
            return true;
        }
        return this.asLocalDate().isBefore(minDate) || this.asLocalDate().isAfter(LocalDate.now());
    }

    public boolean isBeforeNumDays(int days) {
        if (date == null) {
            return true;
        }
        if (!this.isValidDate()) {
            return true;
        }
        return this.asLocalDate().isBefore(LocalDate.now().minusDays(days));
    }

    public boolean isReasonable() {
        if (date == null) {
            return false;
        }
        return this.asLocalDate().isAfter(MIN_DATE.asLocalDate()) && this.asLocalDate().isBefore(MAX_DATE.asLocalDate());
    }

    public boolean isBeforeBeginningOfLastYear() {
        return date != null
            && this.asLocalDate().isBefore(LocalDate.ofYearDay(LocalDate.now().getYear() - 1, 1));
    }

    public boolean isBefore(LocalDate providedDate) {
        return date != null
            && this.asLocalDate().isBefore(providedDate);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof InternalDate)) {
            return false;
        }
        final InternalDate that = (InternalDate) object;
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
