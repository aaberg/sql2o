package org.sql2o.extensions.postgres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.quirks.PostgresQuirks;
import org.postgresql.util.PGInterval;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What each temporal type of postgres looks like by the time it reaches the mapper, and what can be done with it. Needs
 * {@code docker compose up -d postgres-db}.
 *
 * <p>The interesting cases are the two that carry a zone. A timestamp with a zone is fine as a
 * {@link java.sql.Timestamp}, because postgres normalises it to an instant on the way in and the original offset is
 * not in the column to begin with. A time with a zone is not, because the driver hands it over as a
 * {@link java.sql.Time} with the offset already folded into the zone of the session, which is why
 * {@link PostgresQuirks#getRSVal} asks the result set for an {@link OffsetTime} instead.
 *
 * <p>Values are asserted against the instant or the wall clock rather than a rendering, because postgres resolves a
 * zone against the session and the session is not something the test controls.
 */
public class PostgresTemporalTypesTest {

    private static final String URL = "jdbc:postgresql://localhost:15432/postgres";

    private Sql2o sql2o;

    @BeforeEach
    void setUp() {
        sql2o = new Sql2o(URL, "testuser", "testpassword", new PostgresQuirks());
    }

    static Stream<Arguments> temporalColumns() {
        return Stream.of(
                Arguments.of("timestamp", "timestamp '2020-01-01 12:34:56.789'", "2020-01-01 12:34:56.789",
                        "java.sql.Timestamp"),
                // The offset a timestamp was written with is not in the column, so the instant is all there is.
                Arguments.of("timestamptz", "timestamptz '2020-01-01 12:34:56.789+02'", null,
                        "java.sql.Timestamp"),
                Arguments.of("date", "date '2020-01-01'", "2020-01-01", "java.sql.Date"),
                // java.sql.Time has no milliseconds, so the rendering of a time column never shows any.
                Arguments.of("time", "time '12:34:56.789'", "12:34:56", "java.sql.Time"),
                // What the driver hands over is the lossy shape; the quirks turns it into an OffsetTime.
                Arguments.of("timetz", "timetz '12:34:56.789+02'", null, "java.sql.Time"),
                Arguments.of("interval", "interval '1 year 2 months 3 days 4:05:06'",
                        "1 years 2 mons 3 days 4 hours 5 mins 6 secs", "org.postgresql.util.PGInterval"),
                Arguments.of("interval year to month", "interval '1 year 2 months'",
                        "1 years 2 mons", "org.postgresql.util.PGInterval")
        );
    }

    /**
     * The class the driver itself hands over, before any of sql2o sees it. That is the lossy {@link java.sql.Time} for
     * a time with a zone, which is the reason {@link PostgresQuirks#getRSVal} exists.
     */
    @ParameterizedTest(name = "a {0} column arrives from the driver as {3}")
    @MethodSource("temporalColumns")
    public void theDriverHandsOverTheDocumentedClass(String columnType, String expression, String rendered,
                                                     String expectedClassName) throws SQLException {
        assertEquals(expectedClassName, readRawObject(expression).getClass().getName());
    }

    /** The rendering the driver produces, which is also what a String target gets. */
    @ParameterizedTest(name = "a {0} column reads as {2}")
    @MethodSource("temporalColumns")
    public void theColumnRendersAsDocumented(String columnType, String expression, String rendered,
                                              String expectedClassName) throws SQLException {
        if (rendered == null) {
            return;
        }

        assertEquals(rendered, scalarAs(String.class, expression));
    }

    @Test
    public void aTimestampColumnReadsAsTheWholeJdkDateFamily() {
        String expression = "timestamp '2020-01-01 12:34:56.789'";
        LocalDateTime wallClock = LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789_000_000);

        assertEquals(wallClock, scalarAs(LocalDateTime.class, expression));
        assertEquals(wallClock.toLocalDate(), scalarAs(LocalDate.class, expression));
        assertEquals(wallClock.toLocalTime(), scalarAs(LocalTime.class, expression));
        assertEquals(wallClock, scalarAs(java.sql.Timestamp.class, expression).toLocalDateTime());
        assertEquals(wallClock.atZone(ZoneId.systemDefault()).toInstant(), scalarAs(Instant.class, expression));
    }

    @Test
    public void aTimestampWithAZoneColumnKeepsItsInstant() {
        String expression = "timestamptz '2020-01-01 12:34:56.789+02'";
        Instant expected = OffsetDateTime.parse("2020-01-01T12:34:56.789+02:00").toInstant();

        // The column holds an instant and not the offset it was written with, so this is all that is worth asserting.
        assertEquals(expected, scalarAs(Instant.class, expression));
        assertEquals(expected, scalarAs(java.sql.Timestamp.class, expression).toInstant());
        assertEquals(expected,
                scalarAs(OffsetDateTime.class, expression).toInstant());
    }

    @Test
    public void aDateColumnReadsAsADate() {
        String expression = "date '2020-01-01'";

        assertEquals(LocalDate.of(2020, 1, 1), scalarAs(LocalDate.class, expression));
        assertEquals("2020-01-01", scalarAs(java.sql.Date.class, expression).toString());
        assertEquals("2020-01-01", scalarAs(Date.class, expression).toString());
    }

    @Test
    public void aTimeColumnReadsAsATime() {
        String expression = "time '12:34:56.789'";

        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), scalarAs(LocalTime.class, expression));
        // java.sql.Time has no milliseconds to keep, so the value comes back without them.
        assertEquals(LocalTime.of(12, 34, 56), scalarAs(java.sql.Time.class, expression).toLocalTime());
    }

    /**
     * The point of the whole exercise: the offset survives, which it does not when the driver is left to hand over a
     * java.sql.Time. The driver reports both this and a plain time as Types.TIME, so the quirks has to go by the name of
     * the type to tell them apart.
     */
    @Test
    public void aTimeWithAZoneColumnKeepsItsOffset() {
        String expression = "timetz '12:34:56.789+02'";

        OffsetTime offsetTime = scalarAs(OffsetTime.class, expression);

        assertEquals(ZoneOffset.ofHours(2), offsetTime.getOffset());
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), offsetTime.toLocalTime());
    }

    /** A plain time must not be mistaken for one with a zone. */
    @Test
    public void aPlainTimeColumnIsNotGivenAnOffset() {
        Object value = scalarAs(Object.class, "time '12:34:56.789'");

        assertThat(value, org.hamcrest.Matchers.instanceOf(java.sql.Time.class));
    }

    /** And the fields a mapper asks for keep working, whichever of the shapes the wall clock arrives in. */
    @Test
    public void aTimeWithAZoneColumnStillFillsThePlainTypes() {
        String column = "timetz '12:34:56.789+02'";

        try (Connection connection = sql2o.open()) {
            Times times = connection.createQuery("select " + column + " as offsetTime, " + column
                    + " as localTime, " + column + " as sqlTime, " + column + " as text")
                    .executeAndFetchFirst(Times.class);

            assertEquals(LocalTime.of(12, 34, 56, 789_000_000), times.offsetTime.toLocalTime());
            assertEquals(ZoneOffset.ofHours(2), times.offsetTime.getOffset());
            assertEquals(LocalTime.of(12, 34, 56, 789_000_000), times.localTime);
            assertEquals(LocalTime.of(12, 34, 56), times.sqlTime.toLocalTime());
            assertEquals("12:34:56.789+02:00", times.text);
        }
    }

    @Test
    public void anIntervalColumnReadsAsADriverType() {
        String expression = "interval '1 year 2 months 3 days 4:05:06'";

        assertThat(scalarAs(Object.class, expression), org.hamcrest.Matchers.instanceOf(PGInterval.class));
        assertEquals("1 years 2 mons 3 days 4 hours 5 mins 6 secs", scalarAs(PGInterval.class, expression).toString());
        assertEquals("1 years 2 mons 3 days 4 hours 5 mins 6 secs", scalarAs(String.class, expression));
    }

    /** The driver will not convert a temporal column to a string itself, so sql2o has to. */
    @Test
    public void aTemporalColumnCannotBeReadAsAStringByTheDriverAlone() throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "testuser", "testpassword");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select timetz '12:34:56.789+02' val")) {
            resultSet.next();

            SQLException thrown = assertThrows(SQLException.class, () -> resultSet.getObject(1, String.class));

            assertThat(thrown.getMessage(), containsString("timetz"));
        }
    }

    @Test
    public void aNullTemporalColumnReadsAsNull() {
        assertNull(scalarAs(String.class, "cast(null as timestamp)"));
        assertNull(scalarAs(String.class, "cast(null as timetz)"));
        assertNull(scalarAs(String.class, "cast(null as interval)"));
    }

    private <T> T scalarAs(Class<T> type, String expression) {
        try (Connection connection = sql2o.open()) {
            return connection.createQuery("select " + expression + " val").executeScalar(type);
        }
    }

    private Object readRawObject(String expression) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "testuser", "testpassword");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select " + expression + " val")) {
            resultSet.next();
            return resultSet.getObject(1);
        }
    }

    public static class Times {
        public OffsetTime offsetTime;
        public LocalTime localTime;
        public java.sql.Time sqlTime;
        public String text;
    }
}