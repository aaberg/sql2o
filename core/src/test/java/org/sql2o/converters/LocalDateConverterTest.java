package org.sql2o.converters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.Sql2o;
import org.sql2o.TestDatabasesArgumentSourceProvider;

import java.time.LocalDate;
import java.time.ZoneOffset;

public class LocalDateConverterTest {

    @Test
    public void convert_sqlDate_returnsLocalDate() throws ConverterException {
        // setup
        final var converter = new LocalDateConverter();

        // test
        final var epochMillis = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        final var inputDate = new java.sql.Date(epochMillis);
        final var convertedDate = converter.convert(inputDate);

        // assert
        assertEquals(LocalDate.of(2024, 1, 1), convertedDate);
    }

    @Test
    public void convert_epochMillis_returnsLocalDate() throws ConverterException {
        // setup
        final var converter = new LocalDateConverter();

        // test
        final var epochMillis = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        final var convertedDate = converter.convert(epochMillis);

        // assert
        assertEquals(LocalDate.of(2024, 1, 1), convertedDate);
    }

    @Test
    public void convert_validDateString_returnsLocalDate() throws ConverterException {
        // setup
        final var converter = new LocalDateConverter();

        // test
        final var convertedDate = converter.convert("2024-01-01");

        // assert
        assertEquals(LocalDate.of(2024, 1, 1), convertedDate);
    }

    @Test
    public void convert_invalidDateString_throwsException() {
        // setup
        final var converter = new LocalDateConverter();

        // test
        assertThrows(ConverterException.class, () -> converter.convert("invalid-date"));
    }


    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(TestDatabasesArgumentSourceProvider.class)
    public void insertAndFetch_usingLocalDateType_isSuccessfull(String dbName, String url, String user, String pass) {
        // setup
        var sql2o = setupDatabase(url, user, pass);

        final var dateToInsert = LocalDate.of(2024, 1, 1);

        // test insert
        try (org.sql2o.Connection con = sql2o.open()) {
            con.createQuery("INSERT INTO java_time_localdate_test_table (id, date) VALUES (:id, :date)")
                .addParameter("id", 1)
                .addParameter("date", dateToInsert)
                .executeUpdate();
        }

        // test fetch
        LocalDatePojo pojo;
        try (var con = sql2o.open()) {
            pojo = con.createQuery("SELECT id, date FROM java_time_localdate_test_table WHERE id = 1")
                    .executeAndFetchFirst(LocalDatePojo.class);

        }

        // assert
        assertNotNull(pojo);
        assertEquals(1, pojo.getId());
        assertEquals(dateToInsert, pojo.getDate());
    }

    @Test
    public void convert_null_returns_null() throws ConverterException {
        // setup
        final var converter = new LocalDateConverter();

        // test
        final var convertedDate = converter.convert(null);
        assertNull(convertedDate);
    }

    private Sql2o setupDatabase(String url, String user, String pass) {

        Sql2o sql2o = new Sql2o(url, user, pass);

        // create table
        String sql = "CREATE TABLE java_time_localdate_test_table (id INT PRIMARY KEY, date DATE)";
        try (org.sql2o.Connection con = sql2o.open()) {
            con.createQuery(sql).executeUpdate();
        }
        return sql2o;
    }

    static class LocalDatePojo {
        private int id;
        private LocalDate date;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

}
