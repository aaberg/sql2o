package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.TestDatabasesArgumentSourceProvider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class OffsetDateTimeConverterTest {

    @Test
    void convert_noConversionNecessary_returnsOffsetDateTime() throws ConverterException {
        // Arrange
        final var converter = new OffsetDateTimeConverter();
        final var targetTime = OffsetDateTime.of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.UTC);

        // Act
        final var convertedTime = converter.convert(targetTime);

        // Assert
        assertSame(targetTime, convertedTime);
    }

    @Test
    void convert_JavaSqlTimestamp_returnsOffsetDateTime() throws ConverterException {
        // Arrange
        final var converter = new OffsetDateTimeConverter();
        final var targetTime = OffsetDateTime.of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.UTC);

        // Act
        final var convertedVal = converter.convert(java.sql.Timestamp.from(targetTime.toInstant()));

        // Assert
        assertEquals(targetTime.toInstant(), convertedVal.toInstant());
    }

    @Test
    void convert_long_returnsOffsetDateTimeWithDefaultTimeZone() throws ConverterException {
        // Arrange
        final var converter = new OffsetDateTimeConverter();
        final var targetTime = OffsetDateTime
            .of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.UTC)
            .toInstant()
            .atZone(ZoneOffset.systemDefault())
            .toOffsetDateTime();
        final var epochMillis = targetTime.toInstant().toEpochMilli();

        // Act
        final var convertedVal = converter.convert(epochMillis);

        // Assert
        assertEquals(targetTime, convertedVal);
    }

    @Test
    void convert_validDateString_returnsOffsetDateTime() throws ConverterException {
        // Arrange
        final var converter = new OffsetDateTimeConverter();
        final var targetTime = OffsetDateTime.of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.UTC);

        // Act
        final var convertedVal = converter.convert("2024-01-01T00:01:02Z");

        // Assert
        assertEquals(targetTime, convertedVal);
    }

    @Test
    void convert_invalidDateString_throwsException() {
        // Arrange
        final var converter = new OffsetDateTimeConverter();

        // Act & Assert
        final var exception = assertThrows(ConverterException.class, () -> converter.convert("invalid date string"));
        assertEquals("Cannot convert String with value 'invalid date string' to java.time.OffsetDateTime", exception.getMessage());
    }

    @Test
    void convert_invalidType_throwsException() {
        // Arrange
        final var converter = new OffsetDateTimeConverter();

        // Act & Assert
        final var exception = assertThrows(ConverterException.class, () -> converter.convert(123));
        assertEquals("Cannot convert type class java.lang.Integer to java.time.OffsetDateTime", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(TestDatabasesArgumentSourceProvider.class)
    void insertAndFetch_usingOffsetDateTimeUtc_isSuccessfull(String dbName, String url, String user, String pass) {
        // Arrange
        final var sql2o = new Sql2o(url, user, pass);

        try (Connection con = sql2o.open()) {
            con.createQuery("create table javatime_offsetdatetime_test_table (id int primary key, time timestamp with time zone)")
                .executeUpdate();
        }

        final var targetTime = OffsetDateTime.of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.UTC);

        // Act
        OffsetDateTime resultTime;
        try (Connection con = sql2o.open()) {
            con.createQuery("insert into javatime_offsetdatetime_test_table (id, time) values (:id, :time)")
                .addParameter("id", 1)
                .addParameter("time", targetTime)
                .executeUpdate();
            resultTime = con.createQuery("select time from javatime_offsetdatetime_test_table where id = 1").executeScalar(OffsetDateTime.class);
        }

        // Assert
        assertEquals(targetTime, resultTime);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(TestDatabasesArgumentSourceProvider.class)
    void insertAndFetch_usingOffsetDateTimeWithGivenTimeZone_isSuccessfull(String dbName, String url, String user, String pass) {
        // Arrange
        final var sql2o = new Sql2o(url, user, pass);

        try (Connection con = sql2o.open()) {
            con.createQuery("create table javatime_offsetdatetime2_test_table (id int primary key, time timestamp with time zone)")
                .executeUpdate();
        }

        final var targetTime = OffsetDateTime.of(2024, 1, 1, 0, 1, 2, 0, ZoneOffset.ofHours(2));

        // Act
        OffsetDateTime resultTime;
        try (Connection con = sql2o.open()) {
            con.createQuery("insert into javatime_offsetdatetime2_test_table (id, time) values (:id, :time)")
                .addParameter("id", 1)
                .addParameter("time", targetTime)
                .executeUpdate();

            resultTime = con.createQuery("select time from javatime_offsetdatetime2_test_table where id = 1").executeScalar(OffsetDateTime.class);
        }

        // Assert
        assertEquals(targetTime.toInstant(), resultTime.toInstant());
    }
}
