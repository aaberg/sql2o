package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OffsetTimeConverterTest {

    private final OffsetTimeConverter converter = new OffsetTimeConverter();

    /** Whatever offset the jvm is in right now, which is what a value that carries none gets. */
    private static ZoneOffset currentOffset() {
        return OffsetTime.now(ZoneId.systemDefault()).getOffset();
    }

    @Test
    void convert_null_returnsNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    void convert_noConversionNecessary_returnsOffsetTime() throws ConverterException {
        // Arrange
        final var targetTime = OffsetTime.of(12, 1, 2, 0, ZoneOffset.ofHours(2));

        // Act
        final var convertedTime = converter.convert(targetTime);

        // Assert
        assertSame(targetTime, convertedTime);
    }

    @Test
    void convert_localTime_returnsOffsetTimeWithTheZoneOfTheJvm() throws ConverterException {
        // Arrange
        final var targetTime = LocalTime.of(0, 1, 2);

        // Act
        final var convertedTime = converter.convert(targetTime);

        // Assert
        assertEquals(OffsetTime.of(targetTime, currentOffset()), convertedTime);
    }

    @Test
    void convert_sqlTime_returnsOffsetTimeWithTheZoneOfTheJvm() throws ConverterException {
        // Arrange
        final var wallClock = LocalTime.of(0, 1, 2);
        final var sqlTime = new java.sql.Time(
                wallClock.atDate(LocalDate.of(1970, 1, 1)).atOffset(currentOffset()).toInstant().toEpochMilli());

        // Act
        final var convertedTime = converter.convert(sqlTime);

        // Assert
        assertEquals(OffsetTime.of(wallClock, currentOffset()), convertedTime);
    }

    @Test
    void convert_plainDateAndTimestamp_returnTheSameInstantAtTheZoneOfTheJvm() throws ConverterException {
        // A plain java.util.Date, and the java.sql.Timestamp that extends it, both know which instant they mean, down to the
        // millisecond, which is all either of them keeps.
        final var instant = Instant.ofEpochMilli(Instant.now().toEpochMilli());
        final var expectedTime = LocalTime.ofInstant(instant, ZoneId.systemDefault());

        assertEquals(expectedTime, converter.convert(new Date(instant.toEpochMilli())).toLocalTime());
        assertEquals(expectedTime, converter.convert(Timestamp.from(instant)).toLocalTime());
    }

    @Test
    void convert_sqlDate_throwsConverterException() {
        ConverterException thrown = assertThrows(ConverterException.class,
                () -> converter.convert(java.sql.Date.valueOf("2020-01-01")));

        assertThat(thrown.getMessage(), containsString("carries no time of day"));
    }

    @Test
    void convert_long_returnsTheSameWallClockWithTheZoneOfTheJvm() throws ConverterException {
        // Arrange
        final var epochMillis = Instant.now().toEpochMilli();
        final var expectedTime = LocalTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());

        // Act
        final var convertedTime = converter.convert(epochMillis);

        // Assert
        assertEquals(expectedTime, convertedTime.toLocalTime());
        assertEquals(currentOffset(), convertedTime.getOffset());
    }

    @Test
    void convert_validTimeString_returnsOffsetTime() throws ConverterException {
        // Arrange
        final var targetTime = OffsetTime.of(1, 2, 3, 0, ZoneOffset.ofHours(3));

        // Act
        final var convertedTime = converter.convert(targetTime.toString());

        // Assert
        assertEquals(targetTime, convertedTime);
    }

    @Test
    void convert_invalidTimeString_throwsConverterException() {
        assertThrows(ConverterException.class, () -> converter.convert("not a time"));
    }

    @Test
    void convert_unsupportedType_throwsConverterException() {
        ConverterException thrown = assertThrows(ConverterException.class, () -> converter.convert(new Object()));

        assertEquals("Cannot convert type class java.lang.Object to java.time.OffsetTime", thrown.getMessage());
    }
}