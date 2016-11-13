/*
 * Copyright (c) 2014 Lars Aaberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.issues;

import org.junit.Ignore;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.quirks.OracleQuirks;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: ac23513
 * Date: 20.02.13
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class OracleTest {

    private Sql2o sql2o;

    public OracleTest() {
        try {
            Class oracleDriverClass = this.getClass().getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver)oracleDriverClass.newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        this.sql2o = new Sql2o("jdbc:oracle:thin:@//localhost:1521/orcl", "test", "test", new OracleQuirks());
    }

    @Test @Ignore
    public void testForIssue12ErrorReadingClobValue() {
        final String sql = "select to_clob('test') val from dual";

        String val = sql2o.createQuery(sql).executeScalar(String.class);
        assertEquals("test", val);
    }

    @Test @Ignore
    public void testUUiID() {

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        final String ddl = "create table testUUID(id integer primary key, uuidval raw(16))";
        final String insertSql = "insert into testUUID(id, uuidval) values (:id, :val)";
        final String selectSql = "select uuidval from testUUID where id = :id";

        try {


            try (Connection connection = sql2o.open()) {
                connection.createQuery(ddl).executeUpdate();

                Query insertQuery = connection.createQuery(insertSql);
                insertQuery.addParameter("id", 1).addParameter("val", uuid1).executeUpdate();
                insertQuery.addParameter("id", 2).addParameter("val", uuid2).executeUpdate();

                Query selectQuery = connection.createQuery(selectSql);
                UUID uuid1FromDb = selectQuery.addParameter("id", 1).executeScalar(UUID.class);
                UUID uuid2FromDb = selectQuery.addParameter("id", 2).executeScalar(UUID.class);

                assertEquals(uuid1, uuid1FromDb);
                assertEquals(uuid2, uuid2FromDb);
            }

        } catch(Exception e){
            e.printStackTrace();
            fail("test failed. Exception");
        } finally {
            try (Connection con = sql2o.open()) {
                con.createQuery("drop table testUUID").executeUpdate();
            }
        }

    }


    // test is weird. Some versions of Oracle returns a rowid instead of the generated sequence value.
//    @Test
//    public void testForIssue13ProblemWithGetGeneratedKeys() {
//
//        try{
//            sql2o.createQuery("drop sequence fooseq", false).executeUpdate();
//        } catch(Sql2oException ex) {
//            // ignore errors, if objects doesn't exists already.
//            int debug = 0;
//        }
//
//        try{
//            sql2o.createQuery("drop table testtable", false).executeUpdate();
//        } catch(Sql2oException e) {
//            // ignore errors, if objects doesn't exists already.
//            int debug = 0;
//        }
//
//
//        sql2o.createQuery("create sequence fooseq", false).executeUpdate();
//        sql2o.createQuery("create table testtable(id integer primary key, val varchar2(30))", false).executeUpdate();
//
//        Connection connection = null;
//        try {
//            connection = sql2o.beginTransaction();
//
//            String insertSomethingSql = "insert into testtable (id, val) values(fooseq.nextval, :val)";
//            Long generatedKey = connection.createQuery(insertSomethingSql, true).addParameter("val", "foo").executeUpdate().getKey(Long.class);
//
//            Long fetchedKey = connection.createQuery("select id from test_tbl").executeScalar(Long.class);
//
//            assertEquals(generatedKey, fetchedKey);
//        } finally {
//            if (connection != null) {
//                connection.rollback();
//            }
//
//        }
//
//        sql2o.createQuery("drop sequence fooseq", false);
//        sql2o.createQuery("drop table testtable");
//
//
//    }
}
