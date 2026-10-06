package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for the converters that write a {@link java.time} value as the {@link java.sql} one every database accepts,
 * which is what the drivers of postgres, oracle, db2 and derby ask for by registering these instead of the ones of core.
 *
 * <p>What is pinned here is the write only, since the read is the one of {@link InstantConverter} and the rest and is
 * inherited unchanged. The point that matters is what the value becomes: a moment on the timeline with the offset
 * dropped for the two types that carry one, a date as a date for the one that has no time of day, and a time of day as a
 * time of day, which is the one conversion here that loses something a {@link Time} has no room for.
 */
public class InstantToTimestampConverterTest {

    private static final Instant AN_INSTANT = Instant.parse("2020-01-01T09:34:56.789Z");

    private static final OffsetDateTime AN_OFFSET_DATE_TIME =
            LocalDateTime.of(2020, 1, 1, 12, 34, 56).atOffset(ZoneOffset.ofHours(3));

    private static final OffsetTime AN_OFFSET_TIME =
            LocalTime.of(12, 34, 56).atOffset(ZoneOffset.ofHours(3));

    private static final LocalDate A_DATE = LocalDate.of(2020, 1, 1);

    private static final LocalTime A_TIME = LocalTime.of(12, 34, 56, 789000000);

    private static final LocalDateTime A_DATE_AND_TIME = LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789000000);

    private final Converter<Instant> instantConverter = new InstantToTimestampConverter();

    private final Converter<OffsetDateTime> offsetDateTimeConverter = new OffsetDateTimeToTimestampConverter();

    private final Converter<OffsetTime> offsetTimeConverter = new OffsetTimeToTimeConverter();

    private final Converter<LocalDate> localDateConverter = new LocalDateToSqlDateConverter();

    private final Converter<LocalTime> localTimeConverter = new LocalTimeToSqlTimeConverter();

    private final Converter<LocalDateTime> localDateTimeConverter = new LocalDateTimeToTimestampConverter();

    @Test
    public void anInstantIsWrittenAsATimestampOfTheSameMoment() {
        assertEquals(Timestamp.from(AN_INSTANT), instantConverter.toDatabaseParam(AN_INSTANT));
    }

    @Test
    public void anOffsetDateTimeIsWrittenAsATimestampOfTheSameMoment() {
        Timestamp written = (Timestamp) offsetDateTimeConverter.toDatabaseParam(AN_OFFSET_DATE_TIME);

        assertEquals(AN_OFFSET_DATE_TIME.toInstant(), written.toInstant());
    }

    @Test
    public void anOffsetTimeIsWrittenAsATimeOfTheSameWallClock() {
        Time written = (Time) offsetTimeConverter.toDatabaseParam(AN_OFFSET_TIME);

        assertEquals(AN_OFFSET_TIME.toLocalTime(), written.toLocalTime());
    }

    @Test
    public void aLocalDateIsWrittenAsADateOfTheSameDay() {
        assertEquals(Date.valueOf(A_DATE), localDateConverter.toDatabaseParam(A_DATE));
    }

    /**
     * A {@link Time} has no fraction of a second to hold, so the milliseconds are dropped rather than rounded: a driver
     * that took the object itself would have kept them, which is the difference this conversion costs.
     */
    @Test
    public void aLocalTimeIsWrittenAsATimeOfTheSameWallClockWithoutItsMilliseconds() {
        Time written = (Time) localTimeConverter.toDatabaseParam(A_TIME);

        assertEquals(A_TIME.withNano(0), written.toLocalTime());
    }

    @Test
    public void aLocalDateTimeIsWrittenAsATimestampOfTheSameValue() {
        assertEquals(Timestamp.valueOf(A_DATE_AND_TIME), localDateTimeConverter.toDatabaseParam(A_DATE_AND_TIME));
    }

    @Test
    public void nothingIsWrittenForNothing() {
        assertNull(instantConverter.toDatabaseParam(null));
        assertNull(offsetDateTimeConverter.toDatabaseParam(null));
        assertNull(offsetTimeConverter.toDatabaseParam(null));
        assertNull(localDateConverter.toDatabaseParam(null));
        assertNull(localTimeConverter.toDatabaseParam(null));
        assertNull(localDateTimeConverter.toDatabaseParam(null));
    }

    /** An instant has no offset, so the moment it stands for comes back exactly as it went in. */
    @Test
    public void anInstantComesBackFromWhatWasWrittenOfIt() throws ConverterException {
        assertEquals(AN_INSTANT, instantConverter.convert(instantConverter.toDatabaseParam(AN_INSTANT)));
    }

    /** And so does a date and a time, which between them lose nothing a {@code java.sql} type cannot hold. */
    @Test
    public void theOnesWithNoZoneComeBackFromWhatWasWrittenOfThem() throws ConverterException {
        assertEquals(A_DATE, localDateConverter.convert(localDateConverter.toDatabaseParam(A_DATE)));
        assertEquals(A_DATE_AND_TIME, localDateTimeConverter.convert(localDateTimeConverter.toDatabaseParam(A_DATE_AND_TIME)));
        assertEquals(A_TIME.withNano(0), localTimeConverter.convert(localTimeConverter.toDatabaseParam(A_TIME)));
    }

    /** The reading is the one of core, untouched, which is the whole reason these are subclasses. */
    @Test
    public void theReadingIsTheOneOfCore() throws ConverterException {
        assertInstanceOf(InstantConverter.class, instantConverter, "the instant converter is the core one, extended");
        assertInstanceOf(OffsetDateTimeConverter.class, offsetDateTimeConverter);
        assertInstanceOf(OffsetTimeConverter.class, offsetTimeConverter);
        assertInstanceOf(LocalDateConverter.class, localDateConverter);
        assertInstanceOf(LocalTimeConverter.class, localTimeConverter);
        assertInstanceOf(LocalDateTimeConverter.class, localDateTimeConverter);
    }

    /**
     * None of them is handed out by the global registry, since five of the six databases here take the {@code java.time}
     * object as it is and only lose the offset by doing so. A driver that needs one registers it in its own quirks,
     * which is where {@link org.sql2o.quirks.NoQuirks#converterOf(Class)} looks before it looks here.
     */
    @Test
    public void theRegistryKeepsHandingOutTheOnesOfCore() {
        assertSame(InstantConverter.class, Convert.getConverterIfExists(Instant.class).getClass());
        assertSame(OffsetDateTimeConverter.class, Convert.getConverterIfExists(OffsetDateTime.class).getClass());
        assertSame(OffsetTimeConverter.class, Convert.getConverterIfExists(OffsetTime.class).getClass());
        assertSame(LocalDateConverter.class, Convert.getConverterIfExists(LocalDate.class).getClass());
        assertSame(LocalTimeConverter.class, Convert.getConverterIfExists(LocalTime.class).getClass());
        assertSame(LocalDateTimeConverter.class, Convert.getConverterIfExists(LocalDateTime.class).getClass());
    }
}