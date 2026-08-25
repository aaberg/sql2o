package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.stream.Stream;

class SqlTimeConverterTest {

    @ParameterizedTest
    @MethodSource("supportedValues")
    void convert_supportedValues_returnsSqlTime(Object value, java.sql.Time expected) throws ConverterException {
        final var converter = new SqlTimeConverter();

        assertEquals(expected, converter.convert(value));
    }

    private static Stream<Arguments> supportedValues() {
        final var expected = java.sql.Time.valueOf("01:02:03");
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(expected, expected),
            Arguments.of(new Date(expected.getTime()), expected),
            Arguments.of(OffsetTime.of(1, 2, 3, 0, ZoneOffset.UTC), expected),
            Arguments.of(expected.getTime(), expected),
            Arguments.of("01:02:03", expected)
        );
    }

    @Test
    void convert_unsupportedValue_throwsConverterException() {
        final var converter = new SqlTimeConverter();

        assertThrows(ConverterException.class, () -> converter.convert(new Object()));
    }
}
