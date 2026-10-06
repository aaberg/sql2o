package org.sql2o.extensions.derby;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A uuid written to and read from a real derby, through the api a caller uses.
 *
 * <p>The instance is built from nothing but a url, the way a caller builds one, so whichever quirks are found are the
 * ones every user of this driver gets. Derby has no uuid type, so the sixteen bytes are what a {@code varchar(16) for
 * bit data} column holds, and this asks what can be written into it and read back out of it. It does not care where the
 * work happens, in this extension or in the converters of core: what is asserted is the value that ends up in the field.
 *
 * <p>Like the hsqldb extension this one needs nothing running: the database is in memory and the driver ships with the
 * test classpath. Note that Derby keeps an in memory database for as long as the jvm lives, so the tables outlive every
 * connection and each test clears rather than creates.
 */
public class DerbyUuidApiTest {

    private static final String URL = "jdbc:derby:memory:uuidapidb;create=true";

    private static final String TABLE = "DERBYUUIDAPI";

    private static final UUID A_UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    private static final UUID ANOTHER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2oTheWayACallerWould() {
        sql2o = new Sql2o(URL, null, null);

        dropTheTable();

        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table " + TABLE + " (id_col varchar(16) for bit data, label_col varchar(20))")
                    .executeUpdate();
        }
    }

    @AfterAll
    public static void dropTheTable() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table " + TABLE).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }

    /** The typed overload has no branch for a uuid, so this is the call that has to reach the sixteen bytes. */
    @Test
    public void aUuidBoundWithItsTypeNamedComesBack() {
        clear();

        try (Connection connection = sql2o.open()) {
            Query query = connection.createQuery("insert into " + TABLE + " (id_col, label_col) values (:id, :label)");
            query.addParameter("id", UUID.class, A_UUID);
            query.addParameter("label", "typed");
            query.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            assertEquals(A_UUID, connection.createQuery("select id_col from " + TABLE).executeScalar(UUID.class));
        }
    }

    @Test
    public void aUuidBoundWithoutItsTypeNamedComesBack() {
        clear();

        try (Connection connection = sql2o.open()) {
            Query query = connection.createQuery("insert into " + TABLE + " (id_col, label_col) values (:id, :label)");
            query.addParameter("id", A_UUID);
            query.addParameter("label", "untyped");
            query.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            assertEquals(A_UUID, connection.createQuery("select id_col from " + TABLE).executeScalar(UUID.class));
        }
    }

    /** The way an application meets a uuid, as a field rather than as a scalar. */
    @Test
    public void aUuidColumnLandsInAFieldOfTypeUuid() {
        clear();

        try (Connection connection = sql2o.open()) {
            Query query = connection.createQuery("insert into " + TABLE + " (id_col, label_col) values (:id, :label)");
            query.addParameter("id", UUID.class, A_UUID);
            query.addParameter("label", "field");
            query.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            var rows = connection.createQuery("select id_col as theId, label_col as theLabel from " + TABLE)
                    .executeAndFetch(RowWithUuid.class);

            assertEquals(1, rows.size());
            assertEquals(A_UUID, rows.get(0).theId);
        }
    }

    @Test
    public void aNullUuidIsStoredAndReadBackAsNull() {
        clear();

        try (Connection connection = sql2o.open()) {
            Query query = connection.createQuery("insert into " + TABLE + " (id_col, label_col) values (:id, :label)");
            query.addParameter("id", UUID.class, null);
            query.addParameter("label", "null");
            query.executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            assertNull(connection.createQuery("select id_col from " + TABLE).executeScalar(UUID.class));
        }
    }

    /** Two uuids, so that a where clause has something to tell apart. */
    @Test
    public void aUuidFiltersOnItsOwnValue() {
        clear();

        try (Connection connection = sql2o.open()) {
            for (UUID id : new UUID[]{A_UUID, ANOTHER_UUID}) {
                Query query = connection.createQuery("insert into " + TABLE + " (id_col) values (:id)");
                query.addParameter("id", UUID.class, id);
                query.executeUpdate();
            }
        }

        try (Connection connection = sql2o.open()) {
            Query query = connection.createQuery("select count(*) from " + TABLE + " where id_col = :id");
            query.addParameter("id", UUID.class, A_UUID);

            assertEquals(1, query.executeScalar(Integer.class));
        }
    }

    private static void clear() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("delete from " + TABLE).executeUpdate();
        }
    }

    public static class RowWithUuid {
        public UUID theId;
        public String theLabel;
    }
}