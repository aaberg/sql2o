package org.sql2o.extensions.mysql;

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
 * Generated keys, where mysql hands back every identity of a multi row insert, for a plain insert of several rows
 * and for a batch alike.
 */
public class MysqlGeneratedKeysTest {

    private static final String URL = "jdbc:mysql://localhost:13306/testdb";

    private static final String TABLE = "MYSQLGENERATEDKEYS";

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2o() {
        sql2o = new Sql2o(URL, "testuser", "testpassword");

        dropTheTable();

        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table " + TABLE
                    + " (id integer auto_increment primary key, val_col varchar(20))").executeUpdate();
        }
    }

    @AfterAll
    public static void dropTheTables() {
        dropTheTable();
    }

    private static void dropTheTable() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table if exists " + TABLE).executeUpdate();
        }
    }

    private static void clear() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("delete from " + TABLE).executeUpdate();
        }
    }

    /** One insert, three rows, and every one of their keys handed back. */
    @Test
    public void anInsertOfSeveralRowsHandsBackEveryKey() {
        clear();

        String multiInsertSql = "insert into " + TABLE + "(val_col) values ('a val'), ('another val'), ('a third val')";

        Object[] keys;
        try (Connection connection = sql2o.open()) {
            keys = connection.createQuery(multiInsertSql).executeUpdate().getKeys();
        }

        assertNotNull(keys);
        assertEqualsLength(3, keys.length);
    }

    /** And the same for a batch. */
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
