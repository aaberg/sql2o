package org.sql2o.converters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.Sql2o;
import org.sql2o.TestDatabasesArgumentSourceProvider;

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

    @Test
    void convert_null_returns_null() throws ConverterException {
        // setup
        final var converter = new LocalDateTimeConverter();

        // test
        final var convertedTime = converter.convert(null);

        // assert
        assertNull(convertedTime);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(TestDatabasesArgumentSourceProvider.class)
    void insertAndFetch_usingLocalDateTimeType_isSuccessfull(String dbName, String url, String user, String pass) {
        // setup
        final var sql2o = new Sql2o(url, user, pass);
        final var targetTime = LocalDateTime.of(2024, 1, 1, 0, 1, 2);

        try (final var connection = sql2o.open()) {
            connection.createQuery("create table java_time_localdatetime_test_table (id int primary key, time timestamp)")
                .executeUpdate();
        }

        // test insert
        try (final var connection = sql2o.open()) {
            connection.createQuery("INSERT INTO java_time_localdatetime_test_table (id, time) VALUES (:id, :time)")
                .addParameter("id", 1)
                .addParameter("time", targetTime)
                .executeUpdate();
        }

        // test fetch
        LocalDateTime result;
        try (final var connection = sql2o.open()) {
            result = connection.createQuery("SELECT time FROM java_time_localdatetime_test_table WHERE id = 1")
                .executeScalar(LocalDateTime.class);
        }

        // assert
        assertEquals(targetTime, result);
    }
}
