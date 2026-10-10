package org.sql2o.extensions.hsqldb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated keys, where hsqldb is more generous than the default database is.
 *
 * <p>These two assertions used to sit in {@code Sql2oTest} of core behind a branch on the database, which is the reason
 * they are here rather than there: core runs on h2 now, and h2 answers a multi row insert with only the last generated
 * identity, so an assertion that fits hsqldb fails on h2 and an assertion that fits h2 says nothing about hsqldb. Both
 * are asserted where the database they are about can run them.
 */
public class HsqlGeneratedKeysTest {

    private static final String URL = "jdbc:hsqldb:mem:generatedkeys";

    private static final String TABLE = "HSQLGENERATEDKEYS";

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2o() {
        sql2o = new Sql2o(URL, "SA", "");

        dropTheTable();

        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table " + TABLE
                    + " (id integer identity primary key, val_col varchar(20))").executeUpdate();
        }
    }

    @AfterAll
    public static void dropTheTables() {
        dropTheTable();
    }

    private static void dropTheTable() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table " + TABLE).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }

    private static void clear() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("delete from " + TABLE).executeUpdate();
        }
    }

    /** One insert, three rows written by a union, and every one of their keys handed back. */
    @Test
    public void anInsertOfSeveralRowsHandsBackEveryKey() {
        clear();

        String multiInsertSql = "insert into " + TABLE + "(val_col) "
                + "select 'a val' from (values(0)) union select 'another val' from (values(0))";

        Object[] keys;
        try (Connection connection = sql2o.open()) {
            keys = connection.createQuery(multiInsertSql).executeUpdate().getKeys();
        }

        assertNotNull(keys);
        assertEqualsLength(2, keys.length);
    }

    /** And the same for a batch, which is where h2 loses all but the last. */
    @Test
    public void aBatchHandsBackEveryKey() {
        clear();

        List<String> vals = new ArrayList<>(List.of("something1", "something2", "something3"));

        List<Integer> keys;
        try (Connection connection = sql2o.open()) {
            var query = connection.createQuery("insert into " + TABLE + "(val_col) values(:val)", true);
            for (String val : vals) {
                query.addParameter("val", val);
                query.addToBatch();
            }
            keys = query.executeBatch().getKeys(Integer.class);
        }

        assertNotNull(keys);
        assertEqualsLength(vals.size(), keys.size());
    }

    private static void assertEqualsLength(int expected, int actual) {
        assertTrue(actual == expected,
                "expected " + expected + " keys but got " + actual);
    }
}