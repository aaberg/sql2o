package org.sql2o.issues;

import junit.framework.TestCase;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.data.Row;
import org.sql2o.data.Table;
import org.sql2o.issues.pojos.Issue1Pojo;
import org.sql2o.issues.pojos.KeyValueEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 10/17/11
 * Time: 9:02 PM
 * This class is to test for reported issues.
 */
public class IssuesTest extends TestCase {

    private String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private String user = "sa";
    private String pass = "";

    private Sql2o sql2o;

    public void setUp(){
        this.sql2o = new Sql2o(url, user, pass);
    }

    /**
     * Tests for issue #1 https://github.com/aaberg/sql2o/issues/1
     *
     * Issue:
     * I have a case where I need to override/modify the value loaded from db.
     * I want to do this in a setter but the current version of sql2o modifies the property directly.
     *
     * Comment:
     * The priority was wrong. Sql2o would try to set the field first, and afterwards the setter. The priority should be
     * the setter first and the field after.
     */
    public void testSetterPriority(){
        Sql2o sql2o = new Sql2o(url, user, pass);
        Issue1Pojo pojo = sql2o.createQuery("select 1 val").executeAndFetchFirst(Issue1Pojo.class);

        assertEquals(2, pojo.val);

    }

    /**
     *  Tests for issue #2 https://github.com/aaberg/sql2o/issues/2
     *
     *  Issue: NPE - should instead tell what the problem is
     *
     */
    public void testForFieldDoesNotExistException(){
        Sql2o sql2o = new Sql2o(url, user, pass);


        try{
            KeyValueEntity pojo = sql2o.createQuery("select 1 id, 'something' foo").executeAndFetchFirst(KeyValueEntity.class);
        }
        catch(Sql2oException ex){
            assertTrue(ex.getMessage().contains("not found"));
        }
    }


    /**
     *  Tests for issue #3 https://github.com/aaberg/sql2o/issues/3
     *
     *  Issue: If an exception occures in the database, while executing batch update,
     *  the database connection is not closed correctly.
     */
    public void testForConnectionStateAfterBatchException() throws SQLException {
        sql2o.createQuery("create table issue3table(id integer identity primary key, val varchar(5))").executeUpdate();
        
        boolean failed = false;

        Connection connection = sql2o.beginTransaction();

        try{
            connection.createQuery("insert into issue3table(val) values(:val)")
                .addParameter("val", "abcde").addToBatch()
                .addParameter("val", "abcdefg").addToBatch() // should fail
                .addParameter("val", "hello").addToBatch()
                .executeBatch().commit();
        }
        catch(Sql2oException ex){
            failed = true;
            System.out.println("expected exception occured, msg: " + ex.getMessage());
        }

        assertTrue(failed);

        assertTrue("Assert that connection is correctly closed (with transaction)", connection.getJdbcConnection().isClosed() );
        
        // same test, but not in a transaction
        Query query = sql2o.createQuery("insert into issue3table(val) values(:val)")
            .addParameter("val", "abcde").addToBatch()
            .addParameter("val", "abcdefg").addToBatch() // should fail
            .addParameter("val", "hello").addToBatch();
        
        boolean failed2 = false;
        try{
            query.executeBatch();
        }
        catch(Sql2oException ex){
            failed2 = true;
            System.out.println("expected error: " + ex.toString());
        }

        assertTrue(failed2);

        assertTrue("Assert that connection is correctly closed (no transaction)", query.getConnection().getJdbcConnection().isClosed());
            
    }

    /**
     *  Tests for issue #4 https://github.com/aaberg/sql2o/issues/4
     *
     *  NPE when typing wrong column name in row.get(...)
     *  Also, column name should not be case sensitive, if sql2o not is in casesensitive property is false.
     */
    public void testForNpeInRowGet(){
        sql2o.createQuery("create table issue4table(id integer identity primary key, val varchar(20))").executeUpdate();
        
        sql2o.createQuery("insert into issue4table (val) values (:val)")
            .addParameter("val", "something").addToBatch()
            .addParameter("val", "something else").addToBatch()
            .addParameter("val", "hello").addToBatch()
            .executeBatch();
        
        Table table = sql2o.createQuery("select * from issue4table").executeAndFetchTable();

        Row row0 = table.rows().get(0);
        String row0Val = row0.getString("vAl");
        
        assertEquals("something", row0Val);
        
        Row row1 = table.rows().get(1);
        boolean failed = false;
        
        try{
            String row1Value = row1.getString("ahsHashah"); // Should fail with an sql2o exception
        }
        catch(Sql2oException ex){
            failed = true;

            assertTrue(ex.getMessage().startsWith("Column with name 'ahsHashah' does not exist"));
        }

        assertTrue("assert that exception occurred", failed);
                
    }
    
    public static class Issue5POJO{
        public int id;
        public int val;
    }
    
    public static class Issue5POJO2{
        public int id;
        public int val;

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }
    }

    /**
     *  Tests for issue #5 https://github.com/aaberg/sql2o/issues/5
     *  crashes if the POJO has a int field where we try to set a null value
     */
    public void testForNullToSimpeType(){
        sql2o.createQuery("create table issue5table(id integer identity primary key, val integer)").executeUpdate();

        sql2o.createQuery("insert into issue5table(val) values (:val)").addParameter("val", (Object)null).executeUpdate();

        List<Issue5POJO> list1 = sql2o.createQuery("select * from issue5table").executeAndFetch(Issue5POJO.class);
        
        List<Issue5POJO2> list2 = sql2o.createQuery("select * from issue5table").executeAndFetch(Issue5POJO2.class);
        
        assertEquals(1, list1.size());
        assertEquals(1, list2.size());
        assertEquals(0, list1.get(0).val);
        assertEquals(0, list2.get(0).getVal());
    }
}
