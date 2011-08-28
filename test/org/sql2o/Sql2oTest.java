package org.sql2o;

import junit.framework.TestCase;
import sun.text.normalizer.Trie;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/21/11
 * Time: 9:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sql2oTest extends TestCase {

    private String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private String user = "sa";
    private String pass = "";

    private Sql2o sql2o;

    private int insertIntoUsers = 0;

    public void setUp() throws Exception {
        this.sql2o = new Sql2o(this.url, this.user, this.pass);

        HashMap<String, String> defaultColumnMap = new HashMap<String,String>();
        defaultColumnMap.put("ID", "id");
        defaultColumnMap.put("NAME", "name");
        defaultColumnMap.put("EMAIL", "email");
        defaultColumnMap.put("TEXT", "text");
        defaultColumnMap.put("ANUMBER", "aNumber");
        defaultColumnMap.put("ALONGNUMBER", "aLongNumber");
        sql2o.setDefaultColumnMappings(defaultColumnMap);
    }

    public void tearDown() throws Exception {

    }
    
    public void testExecuteUpdate(){

    }

    public void testExecuteAndFetch(){
        createAndFillUserTable();

        Date before = new Date();
        List<User> allUsers = sql2o.createQuery("select * from User").executeAndFetch(User.class);
        Date after = new Date();
        long span = after.getTime() - before.getTime();
        System.out.println(String.format("Fetched %s user: %s ms", insertIntoUsers, span));

        assertTrue(allUsers.size() == insertIntoUsers);
        deleteUserTable();
    }

    public void testExecuteAndFetchWithNulls(){
        String sql =
                "create table testExecWithNullsTbl (" +
                        "id int identity primary key, " +
                        "text varchar(255), " +
                        "aNumber int, " +
                        "aLongNumber bigint)";
        sql2o.createQuery(sql).executeUpdate();


        Query insQuery = sql2o.beginTransaction().createQuery("insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
        insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", (Integer)null).addParameter("lnum", 10L).executeUpdate();
        insQuery.addParameter("text", (String)null).addParameter("number", 21).addParameter("lnum", (Long)null).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
        sql2o.commit();

        List<TestEntity> fetched = sql2o.createQuery("select * from testExecWithNullsTbl").executeAndFetch(TestEntity.class);

        assertTrue(fetched.size() == 5);
        assertNull(fetched.get(2).text);
        assertNotNull(fetched.get(3).text);

        assertNull(fetched.get(1).aNumber);
        assertNotNull(fetched.get(2).aNumber);

        assertNull(fetched.get(2).aLongNumber);
        assertNotNull(fetched.get(3).aLongNumber);
    }

    public void testBatch(){
        sql2o.createQuery(
                "create table User(\n" +
                "id int identity primary key,\n" +
                "name varchar(20),\n" +
                "email varchar(255),\n" +
                "text varchar(100))").executeUpdate();

        String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

        sql2o.beginTransaction().createQuery(insQuery).addParameter("name", "test").addParameter("email", "test@test.com").addParameter("text", "something exciting").addToBatch()
                .addParameter("name", "test2").addParameter("email", "test2@test.com").addParameter("text", "something exciting too").addToBatch()
                .addParameter("name", "test3").addParameter("email", "test3@test.com").addParameter("text", "blablabla").addToBatch()
                .executeBatch().commit();

        deleteUserTable();
    }

    public void testExecuteScalar(){
        createAndFillUserTable();

        Object o = sql2o.createQuery("select text from User where id = 2").executeScalar();
        assertTrue(o.getClass().equals(String.class));

        Object o2 = sql2o.createQuery("select 10").executeScalar();
        assertEquals(o2, 10);

        deleteUserTable();
    }

    public void testBatchNoTransaction(){

        sql2o.createQuery(
                "create table User(\n" +
                "id int identity primary key,\n" +
                "name varchar(20),\n" +
                "email varchar(255),\n" +
                "text varchar(100))").executeUpdate();

        String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

        sql2o.createQuery(insQuery).addParameter("name", "test").addParameter("email", "test@test.com").addParameter("text", "something exciting").addToBatch()
                .addParameter("name", "test2").addParameter("email", "test2@test.com").addParameter("text", "something exciting too").addToBatch()
                .addParameter("name", "test3").addParameter("email", "test3@test.com").addParameter("text", "blablabla").addToBatch()
                .executeBatch();

        deleteUserTable();
    }

    public void testCaseInsensitive(){
        sql2o.createQuery("create table testCI(id2 int primary key, value2 varchar(20), sometext varchar(20))").executeUpdate();

        Query query = sql2o.createQuery("insert into testCI(id2, value2, sometext) values(:id, :value, :someText)");
        for (int i = 0; i < 20; i++){
            query.addParameter("id", i).addParameter("value", "some text " + i).addParameter("someText", "whatever " + i).addToBatch();
        }
        query.executeBatch();

        List<TestCIEntity> ciEntities = sql2o.createQuery("select * from testCI").setCaseSensitive(false).executeAndFetch(TestCIEntity.class);

        assertTrue(ciEntities.size() == 20);


        // test defaultCaseSensitive;
        sql2o.setDefaultCaseSensitive(false);
        List<TestCIEntity> ciEntities2 = sql2o.createQuery("select * from testCI").executeAndFetch(TestCIEntity.class);
        assertTrue(ciEntities2.size() == 20);
    }


    /************** Helper stuff ******************/

    private void createAndFillUserTable(){

        int rowCount = 10000;
        sql2o.createQuery(
                "create table User(\n" +
                "id int identity primary key,\n" +
                "name varchar(20),\n" +
                "email varchar(255),\n" +
                "text varchar(100))").executeUpdate();

        Query insQuery = sql2o.createQuery("insert into User(name, email, text) values (:name, :email, :text)");
        Date before = new Date();
        for (int idx = 0; idx < rowCount; idx++){
            insQuery.addParameter("name", "a name " + idx)
                    .addParameter("email", String.format("test%s@email.com", idx))
                    .addParameter("text", "some text").addToBatch();
        }
        insQuery.executeBatch().commit();
        Date after = new Date();
        Long span = after.getTime() - before.getTime();

        System.out.println(String.format("inserted %d rows into User table. Time used: %s ms", rowCount, span));

        insertIntoUsers += rowCount;
    }

    private void deleteUserTable(){
        sql2o.createQuery("drop table User").executeUpdate();
        insertIntoUsers = 0;
    }
}
