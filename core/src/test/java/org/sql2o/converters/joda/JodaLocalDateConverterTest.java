package org.sql2o.converters.joda;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.sql2o.converters.ConverterException;

import java.sql.Date;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JodaLocalDateConverter}.
 *
 * <p>Everything is pinned as a date and never as a rendered time of day, since a local date has none and because the
 * local date a moment belongs to depends on the zone of the machine running the test.
 */
public class JodaLocalDateConverterTest {

    /** Noon UTC on the first of January, the same instant the other joda tests are written against. */
    private static final long MILLIS = 1577880000000L;

    private static final LocalDate THE_FIRST_OF_JANUARY = new LocalDate(2020, 1, 1);

    private final JodaLocalDateConverter converter = new JodaLocalDateConverter();

    @Test
    public void nothingConvertsToNothing() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aTimestampBecomesTheDateItFallsOn() throws ConverterException {
        assertEquals(THE_FIRST_OF_JANUARY, converter.convert(new Timestamp(MILLIS)));
    }

    @Test
    public void aDateBecomesItself() throws ConverterException {
        assertEquals(THE_FIRST_OF_JANUARY, converter.convert(new Date(MILLIS)));
    }

    @Test
    public void aNumberOfMillisecondsBecomesTheDateItFallsOn() throws ConverterException {
        assertEquals(THE_FIRST_OF_JANUARY, converter.convert(MILLIS));
    }

    /** Unlike a {@link org.joda.time.DateTime}, a local date is a date with no zone on it, and so is what joda can read. */
    @Test
    public void anIsoStringBecomesTheDateItNames() throws ConverterException {
        assertEquals(THE_FIRST_OF_JANUARY, converter.convert("2020-01-01"));
    }

    @Test
    public void aStringThatNamesNoDateIsRefused() {
        assertThrows(ConverterException.class, () -> converter.convert("not a date at all"));
    }

    @Test
    public void aTypeNobodyCanReadIsRefused() {
        assertThrows(ConverterException.class, () -> converter.convert(new Object()));
    }

    /**
     * A {@link Date} rather than a {@link Timestamp}, since a date column is what a local date is meant for and not every
     * driver will take the wider type in one.
     */
    @Test
    public void aDateIsWrittenAsADateRatherThanATimestamp() {
        assertEquals(Date.class, converter.toDatabaseParam(THE_FIRST_OF_JANUARY).getClass());
    }

    @Test
    public void aDateSurvivesTheRoundTrip() throws ConverterException {
        assertEquals(THE_FIRST_OF_JANUARY, converter.convert(converter.toDatabaseParam(THE_FIRST_OF_JANUARY)));
    }
}