package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.H2ArgumentsSourceProvider;
import org.sql2o.Sql2o;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

public class LocalTimeConverterTest {


    @Test
    void convert_sqlTime_returnsLocalTime() throws ConverterException {
        // setup
        final var converter = new LocalTimeConverter();
        final var targetTime = LocalTime.of(0,0,1);

        // test
        final var inputTime = java.sql.Time.valueOf(targetTime);
        final var convertedTime = converter.convert(inputTime);

        // assert
        assertEquals(targetTime, convertedTime);
    }

    @Test
    void convert_sqlTimestamp_returnsLocalTime() throws ConverterException {
        // setup
        final var converter = new LocalTimeConverter();
        final var targetTime = LocalTime.of(0,5,1);

        // test
        final var inputTime = java.sql.Timestamp.valueOf(targetTime.atDate(LocalDate.of(1970, 1, 1)));
        final var convertedTime = converter.convert(inputTime);

        // assert
        assertEquals(targetTime, convertedTime);
    }

    @Test
    void convert_validTimeString_returnsLocalTime() throws ConverterException {
        // setup
        final var converter = new LocalTimeConverter();

        // test
        final var convertedTime = converter.convert("00:00:01");

        // assert
        assertEquals(LocalTime.of(0,0,1), convertedTime);
    }

    @Test
    void convert_sqlTime_keepsTheMillisecondsThatToLocalTimeWouldDrop() throws ConverterException {
        // java.sql.Time holds the fraction of a second in its epoch value while toLocalTime() throws it away.
        final var converter = new LocalTimeConverter();
        final var targetTime = LocalTime.of(0, 1, 2, 123_000_000);
        final var sqlTime = new java.sql.Time(
                targetTime.atDate(LocalDate.of(1970, 1, 1)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        assertEquals(targetTime, converter.convert(sqlTime));
    }

    @Test
    void convert_offsetTime_returnsTheWallClockPartOfIt() throws ConverterException {
        // A driver that keeps the offset of a time with a zone hands over an OffsetTime, and a plain LocalTime means
        // the wall clock part of it.
        final var converter = new LocalTimeConverter();
        final var targetTime = OffsetTime.of(0, 5, 1, 0, ZoneOffset.ofHours(2));

        final var convertedTime = converter.convert(targetTime);

        assertEquals(targetTime.toLocalTime(), convertedTime);
    }

    @Test
    void convert_localTime_returnsItself() throws ConverterException {
        final var converter = new LocalTimeConverter();
        final var targetTime = LocalTime.of(1, 2, 3);

        assertSame(targetTime, converter.convert(targetTime));
    }

    @Test
    void convert_invalidTimeString_throwsConverterException() {
        // setup
        final var converter = new LocalTimeConverter();

        // test
        final var invalidTimeString = "invalid time string";
        final var exception = assertThrows(ConverterException.class, () -> converter.convert(invalidTimeString));

        // assert
        assertTrue(exception.getMessage().contains(invalidTimeString));
    }

    /**
     * This test is run only for H2. The reason is that H2 and HSQLDB handles time zone differently. H2 correctly stores
     * LocalTime as a local time without time zone, while HSQLDB converts the LocalTime to UTC before storing it. For
     * this reason I haven't been able to make one test that works for both HSQLDB and H2.
     * Read more here https://hsqldb.org/doc/2.0/guide/guide.html#sgc_datetime_types
     *
     * <p>HSQLDB is no longer part of the core test classpath at all; its own round trip over the typed parameters lives in
     * {@code HsqlTypedParameterTest} of the hsqldb extension, which leaves a LocalTime out for this same reason.
     * @param dbName
     * @param url
     * @param user
     * @param pass
     */
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(H2ArgumentsSourceProvider.class)
    void insertAndFetch_usingLocalTimeConverter_isSuccessfull(String dbName, String url, String user, String pass) {
        // setup
        final var targetTime = LocalTime.of(1,2,3);

        final var sql2o = new Sql2o(url, user, pass);
        final var createTableSql = "create table java_time_localtime_test_table (id int primary key, time time without time zone)";
        final var insertSql = "insert into java_time_localtime_test_table (id, time) values (:id, :time)";
        try (var con = sql2o.open()) {
            con.createQuery(createTableSql)
                .executeUpdate();
        }

        // Test insert
        try (var con = sql2o.open()) {
            con.createQuery(insertSql)
                .addParameter("id", 1)
                .addParameter("time", targetTime)
                .executeUpdate();
        }

        LocalTimePojo pojo;
        // Test fetch
        try (var con = sql2o.open()) {
            pojo = con.createQuery("select id, time from java_time_localtime_test_table where id = 1")
                .executeAndFetchFirst(LocalTimePojo.class);
        }

        assertEquals(1, pojo.getId());
        assertEquals(targetTime, pojo.getTime());
    }

    @Test
    void convert_null_returns_null() throws ConverterException {
        // setup
        final var converter = new LocalTimeConverter();

        // test
        final var convertedTime = converter.convert(null);

        // assert
        assertNull(convertedTime);
    }

    static class LocalTimePojo {
        private int id;
        private LocalTime time;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }
    }
    @Test
    public void convert_unsupportedType_throwsException() {
        // setup
        final var converter = new LocalTimeConverter();

        // test and assert
        final var ex = assertThrows(ConverterException.class, () -> converter.convert(new Object()));
        assertEquals("Can't convert type java.lang.Object to LocalTime", ex.getMessage());
    }
}
