package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the converters that write a {@link java.time} value as the {@link java.sql} one every database accepts,
 * which is what the drivers of postgres, oracle and db2 ask for by registering these instead of the ones of core.
 *
 * <p>What is pinned here is the write only, since the read is the one of {@link InstantConverter} and the rest and is
 * inherited unchanged. The point that matters is what the value becomes: a moment on the timeline, with the offset
 * dropped, because that is all a timestamp column can hold.
 */
public class InstantToTimestampConverterTest {

    private static final Instant AN_INSTANT = Instant.parse("2020-01-01T09:34:56.789Z");

    private static final OffsetDateTime AN_OFFSET_DATE_TIME =
            LocalDateTime.of(2020, 1, 1, 12, 34, 56).atOffset(ZoneOffset.ofHours(3));

    private static final OffsetTime AN_OFFSET_TIME =
            LocalTime.of(12, 34, 56).atOffset(ZoneOffset.ofHours(3));

    private final Converter<Instant> instantConverter = new InstantToTimestampConverter();

    private final Converter<OffsetDateTime> offsetDateTimeConverter = new OffsetDateTimeToTimestampConverter();

    private final Converter<OffsetTime> offsetTimeConverter = new OffsetTimeToTimeConverter();

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
    public void nothingIsWrittenForNothing() {
        assertNull(instantConverter.toDatabaseParam(null));
        assertNull(offsetDateTimeConverter.toDatabaseParam(null));
        assertNull(offsetTimeConverter.toDatabaseParam(null));
    }

    /** An instant has no offset, so the moment it stands for comes back exactly as it went in. */
    @Test
    public void anInstantComesBackFromWhatWasWrittenOfIt() throws ConverterException {
        assertEquals(AN_INSTANT, instantConverter.convert(instantConverter.toDatabaseParam(AN_INSTANT)));
    }

    /** The reading is the one of core, untouched, which is the whole reason these are subclasses. */
    @Test
    public void theReadingIsTheOneOfCore() throws ConverterException {
        assertInstanceOf(InstantConverter.class, instantConverter, "the instant converter is the core one, extended");
        assertInstanceOf(OffsetDateTimeConverter.class, offsetDateTimeConverter);
        assertInstanceOf(OffsetTimeConverter.class, offsetTimeConverter);
    }
}