package org.sql2o.converters.joda;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.sql2o.converters.ConverterException;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JodaDateTimeConverter}.
 *
 * <p>What matters here is that a moment on the timeline survives the round trip, and that a converter built for a zone
 * answers in it. Both are asserted against a fixed instant whose local time is nowhere near a day boundary either way, so
 * that neither depends on the zone of the machine running them.
 */
public class JodaDateTimeConverterTest {

    /** Noon UTC on the first of January. */
    private static final long MILLIS = 1577880000000L;

    private final JodaDateTimeConverter converter = new JodaDateTimeConverter();

    @Test
    public void nothingConvertsToNothing() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aTimestampBecomesTheMomentItStandsFor() throws ConverterException {
        assertEquals(MILLIS, converter.convert(new Timestamp(MILLIS)).getMillis());
    }

    @Test
    public void aDateBecomesTheMomentItStandsFor() throws ConverterException {
        assertEquals(MILLIS, converter.convert(new Date(MILLIS)).getMillis());
    }

    @Test
    public void aNumberOfMillisecondsBecomesThatMoment() throws ConverterException {
        assertEquals(MILLIS, converter.convert(MILLIS).getMillis());
    }

    /**
     * Joda builds a local time out of the value and has no zone of its own to put it in, so a string carrying an offset
     * is not a local time and is refused. {@link JodaLocalDateConverter} and {@link JodaLocalTimeConverter} do read a
     * string, which is the asymmetry worth pinning.
     */
    @Test
    public void aStringIsRefusedRatherThanGuessedAt() {
        assertThrows(ConverterException.class, () -> converter.convert("2020-01-01T12:00:00.000Z"));
        assertThrows(ConverterException.class, () -> converter.convert("not a moment at all"));
    }

    @Test
    public void aTypeNobodyCanReadIsRefused() {
        assertThrows(ConverterException.class, () -> converter.convert(new Object()));
    }

    @Test
    public void aDateTimeIsWrittenAsATimestampOfTheSameMillis() {
        assertEquals(new Timestamp(MILLIS), converter.toDatabaseParam(new DateTime(MILLIS)));
    }

    @Test
    public void aDateTimeSurvivesTheRoundTrip() throws ConverterException {
        DateTime written = new DateTime(MILLIS, DateTimeZone.UTC);

        DateTime read = converter.convert(converter.toDatabaseParam(written));

        assertEquals(written.getMillis(), read.getMillis());
    }

    /**
     * The zone a converter was built with is the zone its result is in, and it relabels rather than converts: the value
     * is read as a local date and time in the zone of the jvm, and those same fields are then read back in the zone
     * asked for. So the millis move by the difference between the two zones, which is worth knowing before a converter
     * for another zone is registered expecting the moment to survive.
     */
    @Test
    public void aConverterBuiltForAZoneRelabelsTheWallClockRatherThanConvertingTheInstant() throws ConverterException {
        DateTime inTheZoneOfTheJvm = new DateTime(MILLIS);
        DateTime read = new JodaDateTimeConverter(DateTimeZone.UTC).convert(new Timestamp(MILLIS));

        assertEquals(DateTimeZone.UTC, read.getZone());
        assertEquals(inTheZoneOfTheJvm.getYear(), read.getYear());
        assertEquals(inTheZoneOfTheJvm.getMonthOfYear(), read.getMonthOfYear());
        assertEquals(inTheZoneOfTheJvm.getDayOfMonth(), read.getDayOfMonth());
        assertEquals(inTheZoneOfTheJvm.getHourOfDay(), read.getHourOfDay());
        assertEquals(inTheZoneOfTheJvm.getMinuteOfHour(), read.getMinuteOfHour());
    }

    @Test
    public void aConverterBuiltWithNoZoneUsesTheDefaultOne() throws ConverterException {
        DateTime read = new JodaDateTimeConverter().convert(new Timestamp(MILLIS));

        assertEquals(DateTimeZone.getDefault(), read.getZone());
        assertEquals(MILLIS, read.getMillis());
    }
}