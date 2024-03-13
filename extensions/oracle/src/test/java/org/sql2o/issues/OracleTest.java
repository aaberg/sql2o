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
 * User: Lars Aaberg
 * Date: 20.02.13
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class OracleTest {

    private Sql2o sql2o;

    public OracleTest() {
        try {
            Class oracleDriverClass = this.getClass().getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver) oracleDriverClass.newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        this.sql2o = new Sql2o("jdbc:oracle:thin:@localhost:1521:XE", "system", "testpassword", new OracleQuirks());
    }

    @Test
    public void testForIssue12ErrorReadingClobValue() {
        final String sql = "select to_clob('test') val from dual";

        try (Connection con = sql2o.open()) {
            String val = con.createQuery(sql).executeScalar(String.class);
            assertEquals("test", val);
        }

    }

    @Test
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

                UUID uuid1FromDb = connection.createQuery(selectSql).addParameter("id", 1).executeScalar(UUID.class);
                UUID uuid2FromDb = connection.createQuery(selectSql).addParameter("id", 2).executeScalar(UUID.class);

                assertEquals(uuid1, uuid1FromDb);
                assertEquals(uuid2, uuid2FromDb);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("test failed. Exception");
        } finally {
            try (Connection con = sql2o.open()) {
                con.createQuery("drop table testUUID").executeUpdate();
            }
        }

    }
}
