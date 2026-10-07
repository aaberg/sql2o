package org.sql2o.converters.joda;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.converters.Convert;
import org.sql2o.converters.ConverterException;
import org.sql2o.tools.FeatureDetector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The joda converters where they actually have to work: handed out by the registry, and round tripped through a database
 * into a field of a joda type, which is the way a caller meets one.
 *
 * <p>{@code JodaEntity} in the core tests already covers a {@link DateTime} through a database, so the two types it does
 * not are the ones here.
 */
public class JodaThroughTheApiTest {

    /**
     * The delay keeps the database alive once the connection that created it is closed, which the tests here do before
     * reading. Without it the second {@code try} block opens an empty database.
     */
    private static final String URL = "jdbc:h2:mem:jordathroughapi;DB_CLOSE_DELAY=-1";

    /** A row written as three joda types and read back into three fields of those types. */
    public static class JodaRow {
        public long id;
        public DateTime theMoment;
        public LocalDate theDay;
        public LocalTime theClock;
        /** Read as a {@link java.time.LocalDate}, since the column it comes from is written as one. */
        public java.time.LocalDate theJavaDay;
    }

    @Test
    public void theRegistryHandsOutTheJodaConvertersWhenJodaIsThere() throws ConverterException {
        assertTrue(FeatureDetector.isJodaTimeAvailable(), "these tests are pointless without joda on the classpath");

        assertInstanceOf(JodaDateTimeConverter.class, Convert.getConverter(DateTime.class));
        assertInstanceOf(JodaLocalDateConverter.class, Convert.getConverter(LocalDate.class));
        assertInstanceOf(JodaLocalTimeConverter.class, Convert.getConverter(LocalTime.class));
    }

    /**
     * The joda types and the {@code java.time} ones share their simple names, so a reader may be wondering whether both
     * really are registered or whether one silently took the place of the other.
     */
    @Test
    public void theJavaTimeOnesAreRegisteredAlongsideThemRatherThanInsteadOfThem() throws ConverterException {
        assertInstanceOf(org.sql2o.converters.InstantConverter.class, Convert.getConverter(java.time.Instant.class));
        assertInstanceOf(org.sql2o.converters.LocalDateConverter.class, Convert.getConverter(java.time.LocalDate.class));
        assertInstanceOf(org.sql2o.converters.LocalTimeConverter.class, Convert.getConverter(java.time.LocalTime.class));
    }

    @Test
    public void everyJodaTypeGoesIntoADatabaseAndComesBackIntoItsOwnField() {
        DateTime moment = new DateTime();
        LocalDate day = new LocalDate(2020, 1, 1);
        LocalTime clock = new LocalTime(12, 34, 56);

        Sql2o sql2o = new Sql2o(URL, "sa", "");

        drop(sql2o, "JORDATHROUGHAPI");
        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table JORDATHROUGHAPI"
                    + " (id bigint primary key, moment_col timestamp, day_col date, clock_col time)").executeUpdate();

            Query insert = connection.createQuery(
                    "insert into JORDATHROUGHAPI values (:id, :moment, :day, :clock)");
            insert.addParameter("id", 1L);
            insert.addParameter("moment", moment);
            insert.addParameter("day", day);
            insert.addParameter("clock", clock);
            insert.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            JodaRow read = connection.createQuery(
                    "select id, moment_col as theMoment, day_col as theDay, clock_col as theClock from JORDATHROUGHAPI")
                    .executeAndFetchFirst(JodaRow.class);

            assertEquals(moment.getMillis(), read.theMoment.getMillis());
            assertEquals(day, read.theDay);
            assertEquals(clock, read.theClock);
        }

        drop(sql2o, "JORDATHROUGHAPI");
    }

    /**
     * The two of them that share a name are told apart by the value and not by the declared type, which is the part a
     * caller is most likely to get wrong: both go into a date column, and only the joda one comes back as a joda value.
     */
    @Test
    public void theJodaAndTheJavaTimeOnesAreToldApartByWhatIsWritten() {
        Sql2o sql2o = new Sql2o(URL, "sa", "");

        drop(sql2o, "JORDANAMEDALIKES");
        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table JORDANAMEDALIKES"
                    + " (id bigint primary key, joda_day_col date, java_day_col date)").executeUpdate();

            Query insert = connection.createQuery(
                    "insert into JORDANAMEDALIKES values (:id, :joda, :java)");
            insert.addParameter("id", 1L);
            insert.addParameter("joda", new LocalDate(2001, 2, 3));
            insert.addParameter("java", java.time.LocalDate.of(2004, 5, 6));
            insert.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            JodaRow read = connection.createQuery("select id,"
                    + " joda_day_col as theDay, java_day_col as theJavaDay from JORDANAMEDALIKES")
                    .executeAndFetchFirst(JodaRow.class);

            assertEquals(new LocalDate(2001, 2, 3), read.theDay);
            assertEquals(java.time.LocalDate.of(2004, 5, 6), read.theJavaDay);
        }

        drop(sql2o, "JORDANAMEDALIKES");
    }

    private static void drop(Sql2o sql2o, String table) {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table " + table).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }
}