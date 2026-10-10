package org.sql2o.converters.joda;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.sql2o.converters.ConverterException;

import java.sql.Timestamp;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JodaLocalTimeConverter}.
 *
 * <p>This converter has a branch of its own for a {@link OffsetTime}, and that branch does not agree with the general
 * one about what the time of day of a moment is, so the two are pinned apart rather than against each other. And it is
 * pinned that a time of day is written onto *today's* date, which is not what a reader would guess from a method that is
 * handed nothing but a time.
 */
public class JodaLocalTimeConverterTest {

    private final JodaLocalTimeConverter converter = new JodaLocalTimeConverter();

    @Test
    public void nothingConvertsToNothing() throws ConverterException {
        assertNull(converter.convert(null));
    }

    /**
     * The branch of its own, which takes the wall clock rather than going by a zone: a time carrying an offset says what
     * the clock read at that offset was, and that is what a time of day is.
     */
    @Test
    public void aTimeWithAnOffsetBecomesTheTimeOnTheClock() throws ConverterException {
        OffsetTime aTime = OffsetTime.of(1, 2, 3, 456_000_000, ZoneOffset.UTC);

        assertEquals(new LocalTime(1, 2, 3, 456), converter.convert(aTime));
    }

    /**
     * A timestamp is a moment instead, and the time of day a moment belongs to depends on the zone it is read in — here
     * the one of the jvm. That is the other half of why the branch above cannot be folded into this path.
     */
    @Test
    public void aTimestampIsReadAsTheTimeOfDayItFallsOnInTheZoneOfTheJvm() throws ConverterException {
        long aMoment = new DateTime().getMillis();

        assertEquals(LocalTime.fromMillisOfDay(new DateTime(aMoment).getMillisOfDay()),
                converter.convert(new Timestamp(aMoment)));
    }

    @Test
    public void anIsoStringBecomesTheTimeItNames() throws ConverterException {
        assertEquals(new LocalTime(12, 34, 56), converter.convert("12:34:56"));
    }

    @Test
    public void aStringThatNamesNoTimeIsRefused() {
        assertThrows(ConverterException.class, () -> converter.convert("not a time at all"));
    }

    @Test
    public void aTypeNobodyCanReadIsRefused() {
        assertThrows(ConverterException.class, () -> converter.convert(new Object()));
    }

    /**
     * A time of day says nothing about which day it is, so the converter puts it on today's date and hands over a
     * {@link Timestamp} rather than a {@link java.sql.Time}. That is a real fact about what is written rather than an
     * oversight, and it is worth having written down before somebody stores a joda time in a date column and wonders.
     */
    @Test
    public void aTimeIsWrittenOnTodaysDate() {
        Timestamp written = (Timestamp) converter.toDatabaseParam(new LocalTime(12, 34, 56));

        assertEquals(new LocalTime(12, 34, 56), new LocalTime(written.getTime()));
        assertEquals(new DateTime().toLocalDate(), new DateTime(written.getTime()).toLocalDate());
    }

    @Test
    public void aTimeSurvivesTheRoundTrip() throws ConverterException {
        LocalTime written = new LocalTime(12, 34, 56, 789);

        assertEquals(written, converter.convert(converter.toDatabaseParam(written)));
    }
}