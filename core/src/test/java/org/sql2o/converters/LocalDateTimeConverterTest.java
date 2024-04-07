package org.sql2o.converters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


public class LocalDateTimeConverterTest {

    @Test
    void convert_sqlTimestamp_returnsLocalDateTime() throws ConverterException {
        // setup
        final var converter = new LocalDateTimeConverter();
        final var targetTime = LocalDateTime.of(2024, 1, 1, 0, 1, 2);

        // test
        final var inputTime = java.sql.Timestamp.valueOf(targetTime);
        final var convertedTime = converter.convert(inputTime);

        // assert
        assertEquals(targetTime, convertedTime);
    }

    @
    Test
    void convert_epochMillis_returnsLocalDateTime() throws ConverterException {
        // setup
        final var converter = new LocalDateTimeConverter();
        final var targetTime = LocalDateTime.of(2024, 1, 1, 0, 1, 2);

        // test
        final long inputTime = targetTime
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli();
        final var convertedTime = converter.convert(inputTime);

        // assert
        assertEquals(targetTime, convertedTime);
    }

    @Test
    void convert_validTimeString_returnsLocalDateTime() throws ConverterException {
        // setup
        final var converter = new LocalDateTimeConverter();
        final var targetTime = LocalDateTime.of(2024, 1, 1, 0, 1, 2);

        // test
        final var convertedTime = converter.convert("2024-01-01T00:01:02");

        // assert
        assertEquals(targetTime, convertedTime);
    }

    @Test
    void convert_invalidTimeString_throwsException() {
        // setup
        final var converter = new LocalDateTimeConverter();

        // test
        assertThrows(ConverterException.class, () -> converter.convert("invalid"));
    }



}
