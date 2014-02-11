package org.sql2o.issues;

import oracle.jdbc.driver.OracleDriver;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Ignore;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.StatementRunnable;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
            DriverManager.registerDriver(new OracleDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // new oracle developer day VM
        this.sql2o = new Sql2o("jdbc:oracle:thin:@//localhost:1521/PDB1", "test", "test");

        // older oracle developer day VM
        //this.sql2o = new Sql2o("jdbc:oracle:thin:@localhost:1521:orcl", "test", "test");
    }

    /**
     * Issue #8
     * Cannot convert type class oracle.sql.TIMESTAMP to java.util.Date
     *
     *
     * Caused by: org.sql2o.converters.ConverterException: Cannot convert type class oracle.sql.TIMESTAMP to java.util.Date
     * at org.sql2o.converters.DateConverter.convert(DateConverter.java:25)
     * at org.sql2o.converters.DateConverter.convert(DateConverter.java:14)
     * at org.sql2o.reflection.Pojo.setProperty(Pojo.java:84)
     *
     *
     */
    @Test
    public void testForIssue8OracleTimestamps() {
        String sql = "select CURRENT_TIMESTAMP from dual";

        //new TIMESTAMPTZ().timestampValue()

        Date dateVal = sql2o.createQuery(sql).executeScalar(Date.class);
        DateTime dateTimeVal = sql2o.createQuery(sql).executeScalar(DateTime.class);

        assertThat(new DateTime(dateVal).toLocalDate(), is(equalTo(new LocalDate())));
        assertThat(dateTimeVal.toLocalDate(), is(equalTo(new LocalDate())));
    }


    @Test
    public void testForIssue12ErrorReadingClobValue() {
        final String sql = "select to_clob('test') val from dual";

        String val = sql2o.createQuery(sql).executeScalar(String.class);
        assertEquals("test", val);
    }


    // this test requires two objects in the oracle database.
    // create sequence testseq;
    // create table testtable(id integer primary key, val varchar2(30))
//    @Test
//    public void testForIssue13ProblemWithGetGeneratedKeys() {
//
//        Connection connection = null;
//        try {
//            connection = sql2o.beginTransaction();
//
//            Query q = connection.createQuery("create sequence fooseq", false);
//            q.executeUpdate();
//
//            String insertSomethingSql = "insert into testtable (id, val) values(testseq.nextval, :val)";
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
//
//    }
}
