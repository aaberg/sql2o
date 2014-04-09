package org.sql2o.samples.javaedge;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * Created by lars on 09.04.14.
 */
public class AutoClosableTest {


    private final Sql2o sql2o = new Sql2o("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","sa", "");

    @Test
    public void testAutoClosable() {

        final String createSql = "create table testtable(id int identity primary key, val varchar(50))";
        final String insertSql = "insert into testtable(val) values (:val)";
        final String selectSql = "select * fom testtable";
        final String selectCount = "select count(*) cnt from testtable";

        try(Connection con = sql2o.beginTransaction()) {
            // create table and insert something in a transaction

            con.createQuery(createSql).executeUpdate();
            con.createQuery(insertSql).addParameter("val", "foo").executeUpdate();
            con.createQuery(insertSql).addParameter("val", "bar").executeUpdate();

            con.commit();
        }

        Long cnt = (Long)sql2o.createQuery(selectCount).executeScalar();
        assertThat(cnt, is(equalTo(2l)));

        try(Connection con = sql2o.beginTransaction()) {
            con.createQuery(insertSql).addParameter("val", "something").executeUpdate();
            con.createQuery(insertSql).addParameter("val", "We want to").executeUpdate();
            con.createQuery(insertSql).addParameter("val", "rollback").executeUpdate();
            // don't commit, and transaction will be rolled back.
        }

        cnt = (Long)sql2o.createQuery(selectCount).executeScalar();
        assertThat(cnt, is(equalTo(2l)));

    }
}
