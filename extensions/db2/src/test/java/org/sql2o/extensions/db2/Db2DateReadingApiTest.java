package org.sql2o.extensions.db2;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reading a date out of a real db2, through the api a caller uses.
 *
 * <p>The instance is built from nothing but a url, the way a caller builds one, so whichever quirks are found are the
 * ones every user of this driver gets. Db2 hands out DATE, TIME and TIMESTAMP as java.sql.Date, java.sql.Time and
 * java.sql.Timestamp, and this asks what can be read out of that into every shape a field can be declared as. It does
 * not care where the work happens, in this extension or in the converters of core: what is asserted is the value that
 * ends up in the field.
 *
 * <p>Db2 has no types that carry a zone, so these are wall clocks and the assertions do not depend on the time zone of
 * the jvm running them. Needs {@code docker compose up -d db2-ce}, which takes a few minutes to initialise.
 *
 * <p>What cannot be read, and why, is in {@code theConversionsThatNeedTheMissingHalfOfADateAreRefused}.
 */
public class Db2DateReadingApiTest {

    private static final String URL = "jdbc:db2://localhost:50000/testdb";

    private static final String USER = "db2inst1";

    private static final String PASS = "testpassword";

    /** Db2 folds unquoted identifiers to upper case, which the case insensitive default of sql2o absorbs. */
    private static final String TABLE = "DB2DATEREADING";

    /** A row of nulls, since db2 will not let a null be selected, only read back from a column. */
    private static final String NULLS = "DB2DATENULLS";

    private static final String DAY = "2020-01-01";

    private static final String CLOCK = "12:34:56";

    /** Six fractional digits is what a db2 timestamp holds unless the column asks for fewer. */
    private static final String STAMP = "2020-01-01-12.34.56.789123";

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2oTheWayACallerWould() throws Exception {
        DriverManager.registerDriver((Driver) Class.forName("com.ibm.db2.jcc.DB2Driver")
                .getDeclaredConstructor().newInstance());

        sql2o = new Sql2o(URL, USER, PASS);

        dropTheTable(TABLE);
        dropTheTable(NULLS);

        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table " + TABLE + " ("
                    + "day_col date, "
                    + "clock_col time, "
                    + "stamp_col timestamp, "
                    + "rounded_col timestamp(3), "
                    + "old_col date)").executeUpdate();

            connection.createQuery("insert into " + TABLE + " values ("
                    + "date '" + DAY + "', "
                    + "time '" + CLOCK + "', "
                    + "timestamp('" + STAMP + "'), "
                    + "timestamp('" + STAMP + "'), "
                    + "date '1500-01-01')").executeUpdate();

            connection.createQuery("create table " + NULLS + " ("
                    + "day_col date, clock_col time, stamp_col timestamp)").executeUpdate();
            connection.createQuery("insert into " + NULLS + " values (null, null, null)").executeUpdate();
        }
    }

    @AfterAll
    public static void dropTheTables() {
        dropTheTable(TABLE);
        dropTheTable(NULLS);
    }

    /** Db2 has no drop table if exists, so a missing table is an error that has to be swallowed. */
    private static void dropTheTable(String table) {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table " + table).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }

    static Stream<Arguments> targetsForADateColumn() {
        return Stream.of(
                Arguments.of(Date.class, Date.valueOf(DAY)),
                Arguments.of(java.util.Date.class, Date.valueOf(DAY)),
                Arguments.of(Timestamp.class, Timestamp.valueOf(DAY + " 00:00:00")),
                Arguments.of(LocalDate.class, LocalDate.of(2020, 1, 1)),
                Arguments.of(LocalDateTime.class, LocalDateTime.of(2020, 1, 1, 0, 0)),
                Arguments.of(OffsetDateTime.class, LocalDateTime.of(2020, 1, 1, 0, 0).atOffset(offsetOfTheJvm())),
                Arguments.of(String.class, DAY)
        );
    }

    @ParameterizedTest(name = "a DATE column reads as {0}")
    @MethodSource("targetsForADateColumn")
    public void aDateColumnReadsIntoTheUsualTypes(Class<?> target, Object expected) {
        assertEquals(expected, readAs(target, "day_col"));
    }

    /**
     * A date has no time of day, so reading it as one gives midnight of that date rather than of today. The assertion
     * is on the clock rather than on the instant, since the day a time is anchored to is not something a caller wrote.
     */
    @Test
    public void aDateColumnReadAsATimeGivesMidnight() {
        assertEquals(LocalTime.MIDNIGHT, readAs(Time.class, "day_col").toLocalTime());
    }

    /** A time has no date, so it is anchored at the epoch, which is the only day there is to anchor it to. */
    static Stream<Arguments> targetsForATimeColumn() {
        return Stream.of(
                Arguments.of(Time.class, Time.valueOf(CLOCK)),
                Arguments.of(java.util.Date.class, Time.valueOf(CLOCK)),
                Arguments.of(Timestamp.class, Timestamp.valueOf("1970-01-01 " + CLOCK)),
                Arguments.of(LocalTime.class, LocalTime.of(12, 34, 56)),
                Arguments.of(OffsetTime.class, LocalTime.of(12, 34, 56).atOffset(offsetOfTheJvm())),
                Arguments.of(String.class, CLOCK)
        );
    }

    /**
     * A time has no date, so the only day to hang it on is the epoch. A java.sql.Date is an instant like any other,
     * so the time of day is still inside the value a caller receives, and what it renders as is the epoch date.
     */
    @Test
    public void aTimeColumnReadAsADateGivesTheEpochDate() {
        assertEquals("1970-01-01", readAs(Date.class, "clock_col").toString());
    }

    @ParameterizedTest(name = "a TIME column reads as {0}")
    @MethodSource("targetsForATimeColumn")
    public void aTimeColumnReadsIntoTheUsualTypes(Class<?> target, Object expected) {
        assertEquals(expected, readAs(target, "clock_col"));
    }

    /**
     * A timestamp is the one date like column that answers to every shape. The six fractional digits of the value
     * written are expected to arrive whole, since a java.sql.Timestamp has the nanoseconds to hold them.
     */
    static Stream<Arguments> targetsForATimestampColumn() {
        LocalDateTime wallClock = LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789_123_000);
        return Stream.of(
                Arguments.of(Timestamp.class, Timestamp.valueOf("2020-01-01 12:34:56.789123")),
                Arguments.of(java.util.Date.class, Timestamp.valueOf("2020-01-01 12:34:56.789123")),
                Arguments.of(LocalDateTime.class, wallClock),
                Arguments.of(LocalDate.class, LocalDate.of(2020, 1, 1)),
                Arguments.of(LocalTime.class, LocalTime.of(12, 34, 56, 789_123_000)),
                Arguments.of(Instant.class, wallClock.atZone(ZoneId.systemDefault()).toInstant()),
                Arguments.of(OffsetDateTime.class, wallClock.atOffset(offsetOfTheJvm())),
                Arguments.of(OffsetTime.class, LocalTime.of(12, 34, 56, 789_123_000).atOffset(offsetOfTheJvm())),
                Arguments.of(String.class, "2020-01-01 12:34:56.789123")
        );
    }

    @ParameterizedTest(name = "a TIMESTAMP column reads as {0}")
    @MethodSource("targetsForATimestampColumn")
    public void aTimestampColumnReadsIntoTheUsualTypes(Class<?> target, Object expected) {
        assertEquals(expected, readAs(target, "stamp_col"));
    }

    /** The fraction of a second survives the whole way, which is worth stating since a jdk date is usually blamed for
     *  losing it. */
    @Test
    public void aTimestampKeepsEveryFractionalDigitItWasGiven() {
        assertEquals(789_123_000, readAs(Timestamp.class, "stamp_col").getNanos());
    }

    /** And a column declared with fewer digits keeps those, rather than the digits of the value written. */
    @Test
    public void aTimestampColumnWithFewerDigitsKeepsFewerDigits() {
        assertEquals(Timestamp.valueOf("2020-01-01 12:34:56.789"), readAs(Timestamp.class, "rounded_col"));
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789_000_000),
                readAs(LocalDateTime.class, "rounded_col"));
    }

    /**
     * Ibm keeps a page of date and time values that cause problems in jdbc applications, among them the calendar
     * before the 1582 cutover, where the driver is said to adjust what it hands out. A date from before it is written
     * and read back, so that if the adjustment is real it shows up instead of being taken on faith. It does not.
     */
    @Test
    public void aDateFromBeforeTheCalendarCutoverComesBackAsItWasWritten() {
        assertEquals("1500-01-01", readAs(String.class, "old_col"));
        assertEquals(LocalDate.of(1500, 1, 1), readAs(LocalDate.class, "old_col"));
        assertEquals(Date.valueOf("1500-01-01"), readAs(Date.class, "old_col"));
    }

    /**
     * The mapping api, where the names of the properties come from the aliases of the query. The aliases have to be
     * written without underscores: db2 folds them to upper case, and no derivation of names is on by default to turn
     * {@code THE_DAY} back into {@code theDay}.
     */
    @Test
    public void aRowMapsOntoFieldsDeclaredAsDates() {
        String sql = "select day_col as theDay, clock_col as theClock, stamp_col as theStamp from " + TABLE;

        Row row;
        try (Connection connection = sql2o.open()) {
            row = connection.createQuery(sql).executeAndFetchFirst(Row.class);
        }

        assertEquals(LocalDate.of(2020, 1, 1), row.theDay);
        assertEquals(LocalTime.of(12, 34, 56), row.theClock);
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789_123_000), row.theStamp);
    }

    /** The same row into a record, which takes its values through a different builder. */
    @Test
    public void aRowMapsOntoARecordDeclaredAsDates() {
        String sql = "select day_col as theDay, stamp_col as theStamp from " + TABLE;

        Rec rec;
        try (Connection connection = sql2o.open()) {
            rec = connection.createQuery(sql).executeAndFetchFirst(Rec.class);
        }

        assertEquals(LocalDate.of(2020, 1, 1), rec.theDay());
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789_123_000), rec.theStamp());
    }

    /**
     * A null column stays null rather than turning into the epoch. It has to be read out of a column: db2 refuses to
     * select a null at all, answering {@code SQLCODE=-4472} even from a plain statement, so there is nothing to read
     * from a query like {@code select cast(null as date)}.
     */
    @Test
    public void aNullDateStaysNull() {
        assertNull(readAs(LocalDate.class, "day_col", NULLS));
        assertNull(readAs(LocalTime.class, "clock_col", NULLS));
        assertNull(readAs(Timestamp.class, "stamp_col", NULLS));
        assertNull(readAs(Date.class, "day_col", NULLS));
    }

    /**
     * What cannot be read, and why. A time carries no date and a date carries no time of day, so the shapes that
     * require the missing half are refused rather than answered with something invented. Closing one of these gaps
     * will fail this test, which is the point of writing it down.
     */
    @Test
    public void theConversionsThatNeedTheMissingHalfOfADateAreRefused() {
        assertRefused(LocalTime.class, "day_col", "java.sql.Date to LocalTime");
        assertRefused(OffsetTime.class, "day_col", "carries no time of day");
        assertRefused(Instant.class, "day_col", "java.sql.Date to Instant");

        assertRefused(Instant.class, "clock_col", "java.sql.Time to Instant");
        assertRefused(LocalDate.class, "clock_col", "java.sql.Time to java.time.LocalDate");
        assertRefused(LocalDateTime.class, "clock_col", "java.sql.Time to LocalDateTime");
        assertRefused(OffsetDateTime.class, "clock_col", "java.sql.Time to java.time.OffsetDateTime");
    }

    private void assertRefused(Class<?> target, String column, String expectedPartOfTheMessage) {
        Throwable thrown = assertThrows(RuntimeException.class, () -> {
            readAs(target, column);
        });

        String message = String.valueOf(thrown.getMessage()) + " " + thrown.getCause();
        assertEquals(true, message.contains(expectedPartOfTheMessage),
                "expected a refusal mentioning \"" + expectedPartOfTheMessage + "\" but got: " + message);
    }

    /**
     * Reads one column into the given type. Generic rather than taking a Class of an unknown type, so that the value
     * comes back typed and the tables above cannot pair a type with a value of some other one.
     */
    private <T> T readAs(Class<T> target, String column) {
        return readAs(target, column, TABLE);
    }

    private <T> T readAs(Class<T> target, String column, String table) {
        try (Connection connection = sql2o.open()) {
            return connection.createQuery("select " + column + " from " + table).executeScalar(target);
        }
    }

    private static ZoneOffset offsetOfTheJvm() {
        return OffsetDateTime.now(ZoneId.systemDefault()).getOffset();
    }

    public static class Row {
        public LocalDate theDay;
        public LocalTime theClock;
        public LocalDateTime theStamp;
    }

    public record Rec(LocalDate theDay, LocalDateTime theStamp) {}
}