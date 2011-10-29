package org.sql2o.issues;

import junit.framework.TestCase;
import org.junit.Test;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.issues.pojos.Issue1Pojo;
import org.sql2o.issues.pojos.KeyValueEntity;

import java.sql.SQLException;

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
            assertTrue(ex.getMessage().contains("Cannot find property"));
        }
    }
}
