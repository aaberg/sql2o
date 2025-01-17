package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.TestDatabasesArgumentSourceProvider;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class InstantConverterTest {

    @Test
    void convert_sqlTimestamp_returnsInstant() throws ConverterException {
        // Arrange
        final var targetInstant = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        InstantConverter converter = new InstantConverter();
        java.sql.Timestamp val = new java.sql.Timestamp(targetInstant.toEpochMilli());

        // Act
        Instant result = converter.convert(val);

        // Assert
        assertEquals(targetInstant, result);
    }

    @Test
    void convert_epochMillis_returnsInstant() throws ConverterException {
        // Arrange
        final var targetInstant = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        InstantConverter converter = new InstantConverter();

        // Act
        Instant result = converter.convert(targetInstant.toEpochMilli());

        // Assert
        assertEquals(targetInstant, result);
    }

    @Test
    void convert_validDateString_returnsInstant() throws ConverterException {
        // Arrange
        final var targetInstant = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        InstantConverter converter = new InstantConverter();

        // Act
        Instant result = converter.convert("2020-01-01T00:00:00Z");

        // Assert
        assertEquals(targetInstant, result);
    }

    @Test
    void convert_invalidDateString_throwsException() {
        // Arrange
        InstantConverter converter = new InstantConverter();

        // Act & Assert
        final var exception = assertThrows(ConverterException.class, () -> converter.convert("invalid date string"));
        assertEquals("Can't convert string with value 'invalid date string' to java.time.Instant", exception.getMessage());
    }

    @Test
    void convert_invalidType_throwsException() {
        // Arrange
        InstantConverter converter = new InstantConverter();

        // Act & Assert
        final var exception = assertThrows(ConverterException.class, () -> converter.convert(123));
        assertEquals("Can't convert type java.lang.Integer to Instant", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(TestDatabasesArgumentSourceProvider.class)
    void insertAndFetch_usingInstantType_isSuccessfull(String dbName, String url, String user, String pass) {
        // setup
        final var sql2o = new Sql2o(url, user, pass);

        try (Connection con = sql2o.open()) {
            con .createQuery("create table java_time_instant_test_table (id int primary key, instant_col timestamp)")
                .executeUpdate();
        }
        final var instantToInsert = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

        // Test insert
        Instant result;
        try (Connection con = sql2o.open()) {
            con.createQuery("INSERT INTO java_time_instant_test_table (id, instant_col) VALUES (:id, :instant)")
                .addParameter("id", 1)
                .addParameter("instant", instantToInsert)
                .executeUpdate();

        }

        // Test fetch
        try (Connection con = sql2o.open()) {
            result = con.createQuery("SELECT instant_col FROM java_time_instant_test_table WHERE id = 1")
                .executeScalar(Instant.class);
        }

        // Assert
        assertEquals(instantToInsert, result);
    }

    @Test
    void convert_null_returnsNull() throws ConverterException {
        // Arrange
        InstantConverter converter = new InstantConverter();

        // Act
        Instant result = converter.convert(null);

        // Assert
        assertNull(result);
    }
}
