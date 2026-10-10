package org.sql2o.extensions.mysql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unsigned integers, which have no Java counterpart of their own.
 *
 * <p>An {@code integer unsigned} comes back as a {@link Long} and a {@code bigint unsigned} as a
 * {@link BigInteger}, which is what the driver hands out: the values do not fit the signed type of the same width.
 * Both are asserted where the database they are about can run them, since H2 has no unsigned types at all.
 */
public class MysqlUnsignedTest {

    private static final String URL = "jdbc:mysql://localhost:13306/testdb";

    private static final String TABLE = "MYSQLUNSIGNED";

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2o() {
        sql2o = new Sql2o(URL, "testuser", "testpassword");

        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table if exists " + TABLE).executeUpdate();
            connection.createQuery("create table " + TABLE
                    + " (iu_col integer unsigned, biu_col bigint unsigned)").executeUpdate();
            connection.createQuery("insert into " + TABLE + " values (4000000000, 18446744073709551615)")
                    .executeUpdate();
        }
    }

    @AfterAll
    public static void dropTheTables() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table if exists " + TABLE).executeUpdate();
        }
    }

    @Test
    public void anUnsignedIntegerComesBackAsALong() {
        try (Connection connection = sql2o.open()) {
            assertEquals(4000000000L, connection.createQuery("select iu_col from " + TABLE)
                    .executeScalar(Long.class));
        }
    }

    @Test
    public void anUnsignedBigintComesBackAsABigInteger() {
        try (Connection connection = sql2o.open()) {
            assertEquals(new BigInteger("18446744073709551615"),
                    connection.createQuery("select biu_col from " + TABLE).executeScalar(BigInteger.class));
        }
    }

    public static class UnsignedRow {
        public Long iuCol;
        public BigInteger biuCol;
    }

    /** And into fields, the way an application meets them rather than as scalars. */
    @Test
    public void unsignedColumnsLandInFields() {
        try (Connection connection = sql2o.open()) {
            var rows = connection.createQuery("select iu_col as iuCol, biu_col as biuCol from " + TABLE)
                    .executeAndFetch(UnsignedRow.class);

            assertEquals(1, rows.size());
            assertEquals(4000000000L, rows.get(0).iuCol);
            assertEquals(new BigInteger("18446744073709551615"), rows.get(0).biuCol);
        }
    }
}
