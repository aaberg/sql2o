package org.sql2o.issues;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.*;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OracleTest() {
        try {
            Class oracleDriverClass = this.getClass().getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver)oracleDriverClass.newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        // new oracle developer day VM
        this.sql2o = new Sql2o("jdbc:oracle:thin:@//localhost:1521/PDB1", "pmuser", "oracle", QuirksMode.Oracle);

        // older oracle developer day VM
        // this.sql2o = new Sql2o("jdbc:oracle:thin:@localhost:1521:orcl", "test", "test", QuirksMode.Oracle);
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
//    @Test
    public void testForIssue8OracleTimestamps() {
        String sql = "select CURRENT_TIMESTAMP from dual";

        //new TIMESTAMPTZ().timestampValue()

        Date dateVal = sql2o.createQuery(sql).executeScalar(Date.class);
        DateTime dateTimeVal = sql2o.createQuery(sql).executeScalar(DateTime.class);

        assertThat(new DateTime(dateVal).toLocalDate(), is(equalTo(new LocalDate())));
        assertThat(dateTimeVal.toLocalDate(), is(equalTo(new LocalDate())));
    }


//    @Test
    public void testForIssue12ErrorReadingClobValue() {
        final String sql = "select to_clob('test') val from dual";

        String val = sql2o.createQuery(sql).executeScalar(String.class);
        assertEquals("test", val);
    }

//    @Test
    public void testBatch(){
        final String createSql = "create table SomeTable(id int primary key, value varchar2(100))";
        final String dropSql = "drop table SomeTable";

        final String insertSql = "INSERT INTO SomeTable(id, value) VALUES (:id, :value)";
        final String verifySql = "SELECT COUNT(*) FROM SomeTable";

        try{
            sql2o.createQuery(createSql).executeUpdate();

            sql2o.runInTransaction(new StatementRunnable() {
                public void run(Connection connection, Object argument) throws Throwable {
                    Query query = connection.createQuery(insertSql);

                    for (int i = 0; i < 100; i++){
                        query.addParameter("id", i).addParameter("value", "foo" + i).addToBatch();
                    }

                    query.executeBatch();
                }
            });
            int cnt = sql2o.createQuery(verifySql).executeScalar(Integer.class);

            assertThat(cnt, is(equalTo(100)));
        } catch(Exception e) {
            logger.error("error", e);
        } finally {
            sql2o.createQuery(dropSql).executeUpdate();
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
