package org.sql2o.extensions.postgres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Lars Aaberg
 * Date: 1/19/13
 * Time: 10:58 PM
 * Test dedicated for postgres issues. Seems like the postgres jdbc driver behaves somewhat different from other jdbc drivers.
 * This test assumes that there is a local PostgreSQL server with a testdb database which can be accessed by user: test, pass: testtest
 */
public class PostgresTest extends PostgresTestSupport {

    static Stream<PostgresTestDatabase> databases() {
        return PostgresTestDatabase.databases();
    }

    @BeforeEach
    public void announceTheTestClass() {
        logger.info("starting PostgresTest");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testIssue10StatementsOnPostgres_noTransaction(PostgresTestDatabase db) {

        try {
            try (Connection connection = db.getSql2o().open()) {
                connection.createQuery("create table test_table(id SERIAL, val varchar(20))").executeUpdate();
            }

            try (Connection connection = db.getSql2o().open()) {
                Long key = connection.createQuery("insert into test_table (val) values(:val)", true)
                                     .addParameter("val", "something").executeUpdate().getKey(Long.class);
                assertNotNull(key);
                assertTrue(key > 0);

                String selectSql = "select id, val from test_table";
                Table resultTable = connection.createQuery(selectSql).executeAndFetchTable();

                assertThat(resultTable.rows().size(), is(1));
                Row resultRow = resultTable.rows().get(0);
                assertThat(resultRow.getLong("id"), equalTo(key));
                assertThat(resultRow.getString("val"), is("something"));

            }

        } finally {
            try (final Connection connection = db.getSql2o().open();
                 final Query query = connection.createQuery("drop table if exists test_table")) {
                query.executeUpdate();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testIssue10_StatementsOnPostgres_withTransaction(PostgresTestDatabase db) {

        try (final Connection connection = db.getSql2o().beginTransaction()) {

            String createTableSql = "create table test_table(id SERIAL, val varchar(20))";
            connection.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_table (val) values(:val)";
            Long key = connection.createQuery(insertSql, true).addParameter("val", "something").executeUpdate().getKey(Long.class);
            assertNotNull(key);
            assertTrue(key > 0);

            String selectSql = "select id, val from test_table";
            Table resultTable = connection.createQuery(selectSql).executeAndFetchTable();

            assertThat(resultTable.rows().size(), is(1));
            Row resultRow = resultTable.rows().get(0);
            assertThat(resultRow.getLong("id"), equalTo(key));
            assertThat(resultRow.getString("val"), is("something"));
            // always rollback, as this is only for tesing purposes.
            connection.rollback();

        }

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testGetKeyOnSequence(PostgresTestDatabase db) {
        Connection connection = null;

        try {
            connection = db.getSql2o().beginTransaction();

            String createSequenceSql = "create sequence testseq";
            connection.createQuery(createSequenceSql).executeUpdate();

            String createTableSql = "create table test_seq_table (id integer primary key, val varchar(20))";
            connection.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_seq_table(id, val) values (nextval('testseq'), 'something')";
            Long key = connection.createQuery(insertSql, true).executeUpdate().getKey(Long.class);

            assertThat(key, equalTo(1L));

            key = connection.createQuery(insertSql, true).executeUpdate().getKey(Long.class);
            assertThat(key, equalTo(2L));
        } finally {
            if (connection != null) {
                connection.rollback();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testKeyKeyOnSerial(PostgresTestDatabase db) {
        Connection connection = null;

        try {
            connection = db.getSql2o().beginTransaction();

            String createTableSql = "create table test_serial_table (id serial primary key, val varchar(20))";
            connection.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_serial_table(val) values ('something')";
            Long key = connection.createQuery(insertSql, true).executeUpdate().getKey(Long.class);

            assertThat(key, equalTo(1L));

            key = connection.createQuery(insertSql, true).executeUpdate().getKey(Long.class);
            assertThat(key, equalTo(2L));
        } finally {
            if (connection != null) {
                connection.rollback();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testKeysKeyOnSerial(PostgresTestDatabase db) {
        Connection connection = null;

        try {
            connection = db.getSql2o().beginTransaction();

            String createTableSql = "create table test_serial_table (val varchar(20), id serial primary key)";
            connection.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_serial_table(val) values ('something')";
            Object[] key = connection.createQuery(insertSql, true).executeUpdate().getKeys();

            assertThat((String) key[0], equalTo("something"));
            assertThat((Integer) key[1], equalTo(1));

            key = connection.createQuery(insertSql, true).executeUpdate().getKeys();
            assertThat((String) key[0], equalTo("something"));
            assertThat((Integer) key[1], equalTo(2));
        } finally {
            if (connection != null) {
                connection.rollback();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testUUID(PostgresTestDatabase db) {

        Connection connection = null;

        try {
            connection = db.getSql2o().beginTransaction();

            String createSql = "create table uuidtable(id serial primary key, data uuid)";
            connection.createQuery(createSql).executeUpdate();

            UUID uuid = UUID.randomUUID();

            String insertSql = "insert into uuidtable(data) values (:data)";
            connection.createQuery(insertSql).addParameter("data", uuid).executeUpdate();

            String selectSql = "select data from uuidtable";

            UUID fetchedUuid = connection.createQuery(selectSql).executeScalar(UUID.class);

            assertThat(fetchedUuid, is(equalTo(uuid)));
        } finally {
            if (connection != null) {
                connection.rollback();
            }
        }

    }

//    @Test
//    public void testArray() {
//        try (Connection con = sql2o.open()) {
//            con.createQuery("create table arraytest (id serial primary key, data text[])").executeUpdate();
//
//            final var data = new String[]{"foo", "bar", "baz"};
//
//            con.createQuery("insert into arraytest(data) values (:data)")
//                    .addParameter("data", data)
//                    .executeUpdate();
//
//            var fetchedData = con.createQuery("select data from arraytest")
//                    .executeScalar(String[].class);
//
//            assertThat(fetchedData, is(equalTo(data)));
//        }
//    }



}