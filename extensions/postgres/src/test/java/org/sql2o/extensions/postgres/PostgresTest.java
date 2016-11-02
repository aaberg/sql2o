package org.sql2o.extensions.postgres;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.converters.UUIDConverter;
import org.sql2o.data.Row;
import org.sql2o.data.Table;
import org.sql2o.quirks.PostgresQuirks;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: Lars Aaberg
 * Date: 1/19/13
 * Time: 10:58 PM
 * Test dedicated for postgres issues. Seems like the postgres jdbc driver behaves somewhat different from other jdbc drivers.
 * This test assumes that there is a local PostgreSQL server with a testdb database which can be accessed by user: test, pass: testtest
 */
@RunWith(Parameterized.class)
public class PostgresTest extends PostgresTestSupport {

    public PostgresTest(String url, String user, String pass, String testName) {
        super(url, user, pass, testName);
        logger.info("starting PostgresTest");
    }

    @Test
    public void testIssue10StatementsOnPostgres_noTransaction(){

        try {
            String createTableSql = "create table test_table(id SERIAL, val varchar(20))";
            sql2o.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_table (val) values(:val)";
            Long key = (Long)sql2o.createQuery(insertSql, true).addParameter("val", "something").executeUpdate().getKey(Long.class);
            assertNotNull(key);
            assertTrue(key > 0);

            String selectSql = "select id, val from test_table";
            Table resultTable = sql2o.createQuery(selectSql).executeAndFetchTable();

            assertThat(resultTable.rows().size(), is(1));
            Row resultRow = resultTable.rows().get(0);
            assertThat(resultRow.getLong("id"), equalTo(key));
            assertThat(resultRow.getString("val"), is("something"));

        } finally {
            String dropTableSql = "drop table if exists test_table";
            sql2o.createQuery(dropTableSql).executeUpdate();
        }
    }

    @Test
    public void testIssue10_StatementsOnPostgres_withTransaction() {


        Connection connection = null;

        try{
            connection = sql2o.beginTransaction();

            String createTableSql = "create table test_table(id SERIAL, val varchar(20))";
            connection.createQuery(createTableSql).executeUpdate();

            String insertSql = "insert into test_table (val) values(:val)";
            Long key = (Long)connection.createQuery(insertSql, true).addParameter("val", "something").executeUpdate().getKey(Long.class);
            assertNotNull(key);
            assertTrue(key > 0);

            String selectSql = "select id, val from test_table";
            Table resultTable = connection.createQuery(selectSql).executeAndFetchTable();

            assertThat(resultTable.rows().size(), is(1));
            Row resultRow = resultTable.rows().get(0);
            assertThat(resultRow.getLong("id"), equalTo(key));
            assertThat(resultRow.getString("val"), is("something"));

        } finally {

            // always rollback, as this is only for tesing purposes.
            if (connection != null) {
                connection.rollback();
            }
        }


    }

    @Test
    public void testGetKeyOnSequence(){
        Connection connection = null;

        try {
            connection = sql2o.beginTransaction();

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

    @Test
    public void testKeyKeyOnSerial() {
        Connection connection = null;

        try {
            connection = sql2o.beginTransaction();

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

    @Test
    public void testKeysKeyOnSerial() {
        Connection connection = null;

        try {
            connection = sql2o.beginTransaction();

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

    @Test
    public void testUUID() {

        Connection connection = null;

        try {
            connection = sql2o.beginTransaction();

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



}
