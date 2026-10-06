package org.sql2o.quirks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Pins down what the oracle driver hands out for each kind of date and timestamp column, because the extension is built
 * around those classes: {@link OracleQuirks#getRSVal} recognises oracle timestamps by the name prefix, and
 * {@link org.sql2o.converters.OracleDateConverter} has to recognise whatever a {@code Datum} arrives as. A driver that
 * started returning a different class for any of these would break the extension, and quietly at that, since the
 * mapping would fail at run time on a type no test had ever mentioned. Needs {@code docker compose up -d}.
 *
 * <p>Checked against ojdbc 23.3 and Oracle XE 21c. The expected classes are spelled out by name rather than imported, so
 * that a driver change shows up as a readable assertion failure instead of a {@code NoClassDefFoundError}. The value
 * assertions are made against whatever the quirks produce for the same column instead of a hard coded instant, since
 * the zone aware columns resolve against the session time zone and a fixed literal would fail on every machine but one.
 */
public class OracleDriverTypeTest {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";

    private static Sql2o sql2o;

    /**
     * The driver is registered by hand because the url carries the legacy {@code @host:port:SID} form, which the
     * service loader cannot resolve on its own.
     */
    @BeforeAll
    public static void registerTheDriverAndOpenSql2o() {
        try {
            Class<?> oracleDriverClass =
                    OracleDriverTypeTest.class.getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver) oracleDriverClass.getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        sql2o = new Sql2o(URL, "system", "testpassword", new OracleQuirks());
    }

    static Stream<Arguments> dateLikeColumns() {
        return Stream.of(
                Arguments.of("timestamp", "timestamp '2020-01-01 00:00:00'", "oracle.sql.TIMESTAMP"),
                // The zone aware types extend Datum directly instead of extending oracle.sql.TIMESTAMP, which is what
                // used to send them past the date converter and into the base converter, where they failed.
                Arguments.of("timestamp with time zone",
                        "to_timestamp_tz('2020-01-01 00:00:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')",
                        "oracle.sql.TIMESTAMPTZ"),
                Arguments.of("timestamp with local time zone",
                        "cast(timestamp '2020-01-01 00:00:00' as timestamp with local time zone)",
                        "oracle.sql.TIMESTAMPLTZ"),
                // A date column comes back as a plain jdk timestamp, not as an oracle.sql.DATE.
                Arguments.of("date", "date '2020-01-01'", "java.sql.Timestamp")
        );
    }

    /** The subset whose objects can convert themselves, which is what a converter alone can cope with. */
    static Stream<Arguments> selfConvertingColumns() {
        return Stream.of(
                Arguments.of("timestamp", "timestamp '2020-01-01 00:00:00'", "oracle.sql.TIMESTAMP"),
                Arguments.of("date", "date '2020-01-01'", "java.sql.Timestamp")
        );
    }

    /**
     * The zone aware types come back as datums that refuse to convert themselves, with
     * "SQLException: Conversion to Timestamp failed", even though the result set converts them without trouble. A
     * converter holding nothing but the object cannot get a value out of them at all, which is the whole reason the
     * mapping path goes through the quirks now.
     */
    static Stream<Arguments> zoneAwareColumns() {
        return Stream.of(
                Arguments.of("timestamp with time zone",
                        "to_timestamp_tz('2020-01-01 00:00:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')",
                        "oracle.sql.TIMESTAMPTZ"),
                Arguments.of("timestamp with local time zone",
                        "cast(timestamp '2020-01-01 00:00:00' as timestamp with local time zone)",
                        "oracle.sql.TIMESTAMPLTZ")
        );
    }

    @ParameterizedTest(name = "a {0} column reads back as {2}")
    @MethodSource("dateLikeColumns")
    public void theDriverHandsOutTheDocumentedClass(String columnType, String expression, String expectedClassName)
            throws SQLException {
        assertEquals(expectedClassName, readRawObject(expression).getClass().getName());
    }

    @ParameterizedTest(name = "the quirks turns a {0} column into a jdk timestamp")
    @MethodSource("dateLikeColumns")
    public void theQuirksTurnsEveryDateLikeColumnIntoAJdkTimestamp(String columnType, String expression)
            throws SQLException {
        assertThat(readThroughTheQuirks(expression), instanceOf(Timestamp.class));
    }

    /** This is the invariant that broke: the date converter has to cope with whatever the driver hands it. */
    @ParameterizedTest(name = "the date converter reads a {0} column")
    @MethodSource("selfConvertingColumns")
    public void theDateConverterReadsEverySelfConvertingColumn(String columnType, String expression)
            throws SQLException {
        Converter<Date> converter = Convert.getConverterIfExists(Date.class);
        assertNotNull(converter);

        assertEquals(readThroughTheQuirks(expression), readConverting(converter, expression));
    }

    /**
     * And the limitation, pinned so that it is noticed if a driver ever changes: these objects cannot convert
     * themselves, so the converter reports the refusal instead of inventing a value.
     */
    @ParameterizedTest(name = "the date converter reports what a {0} object cannot do")
    @MethodSource("zoneAwareColumns")
    public void theDateConverterReportsTheDriverRefusal(String columnType, String expression) throws SQLException {
        Converter<Date> converter = Convert.getConverterIfExists(Date.class);
        assertNotNull(converter);

        Object raw = readRawObject(expression);

        ConverterException thrown = assertThrows(ConverterException.class, () -> converter.convert(raw));
        assertEquals("Error trying to convert " + raw.getClass().getName() + " to java.util.Date",
                thrown.getMessage());
        assertInstanceOf(SQLException.class, thrown.getCause());
        assertThat(thrown.getCause().getMessage(), containsString("Conversion to Timestamp failed"));
    }

    private static Date readConverting(Converter<Date> converter, String expression) throws SQLException {
        try {
            return converter.convert(readRawObject(expression));
        } catch (ConverterException e) {
            throw new AssertionError("the date converter rejected a " + expression + " column", e);
        }
    }

    /**
     * And the same end to end, because a mapper reads {@code getObject} directly and never asks the quirks for the
     * value, which is the path a user would hit.
     */
    @ParameterizedTest(name = "a {0} column maps onto a java.util.Date property")
    @MethodSource("dateLikeColumns")
    public void everyDateLikeColumnMapsOntoADateProperty(String columnType, String expression) throws SQLException {
        try (Connection connection = sql2o.open()) {
            Date mapped = connection.createQuery("select " + expression + " val from dual")
                    .executeAndFetchFirst(Row.class).val;

            assertNotNull(mapped);
            assertEquals(readThroughTheQuirks(expression), mapped);
        }
    }

    private static Object readRawObject(String expression) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "system", "testpassword");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select " + expression + " val from dual")) {
            resultSet.next();
            return resultSet.getObject(1);
        }
    }

    private static Object readThroughTheQuirks(String expression) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "system", "testpassword");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select " + expression + " val from dual")) {
            resultSet.next();
            return new OracleQuirks().getRSVal(resultSet, 1);
        }
    }

    /** Somewhere to map a single column onto. The field is public because the mapper sets it reflectively. */
    public static class Row {
        public Date val;
    }
}
