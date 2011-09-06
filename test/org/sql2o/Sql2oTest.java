package org.sql2o;

import junit.framework.TestCase;
import org.joda.time.DateTime;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
        sql2o.createQuery("create table testCI(id2 int primary key, value2 varchar(20), sometext varchar(20), valwithgetter varchar(20))").executeUpdate();

        Query query = sql2o.createQuery("insert into testCI(id2, value2, sometext, valwithgetter) values(:id, :value, :someText, :valwithgetter)");
        for (int i = 0; i < 20; i++){
            query.addParameter("id", i).addParameter("value", "some text " + i).addParameter("someText", "whatever " + i).addParameter("valwithgetter", "spaz" + i).addToBatch();
        }
        query.executeBatch();

        List<TestCIEntity> ciEntities = sql2o.createQuery("select * from testCI").setCaseSensitive(false).executeAndFetch(TestCIEntity.class);

        assertTrue(ciEntities.size() == 20);


        // test defaultCaseSensitive;
        sql2o.setDefaultCaseSensitive(false);
        List<TestCIEntity> ciEntities2 = sql2o.createQuery("select * from testCI").executeAndFetch(TestCIEntity.class);
        assertTrue(ciEntities2.size() == 20);
    }

    public void testExecuteAndFetchResultSet() throws SQLException {
        List<Integer> list = sql2o.createQuery("select 1 val union select 2 union select 3").executeScalarList();

        assertEquals((int)list.get(0), 1);
        assertEquals((int)list.get(1), 2);
        assertEquals((int)list.get(2), 3);

    }

    public void testJodaTime(){

        sql2o.createQuery("create table testjoda(id int primary key, joda1 datetime, joda2 datetime)").executeUpdate();

        sql2o.createQuery("insert into testjoda(id, joda1, joda2) values(:id, :joda1, :joda2)")
                .addParameter("id", 1).addParameter("joda1", new DateTime()).addParameter("joda2", new DateTime().plusDays(-1)).addToBatch()
                .addParameter("id", 2).addParameter("joda1", new DateTime().plusYears(1)).addParameter("joda2", new DateTime().plusDays(-2)).addToBatch()
                .addParameter("id", 3).addParameter("joda1", new DateTime().plusYears(2)).addParameter("joda2", new DateTime().plusDays(-3)).addToBatch()
                .executeBatch();

        List<JodaEntity> list = sql2o.createQuery("select * from testjoda").executeAndFetch(JodaEntity.class);

        assertTrue(list.size() == 3);
        assertTrue(list.get(0).getJoda2().isBeforeNow());

    }

    public void testUtilDate(){
        sql2o.createQuery("create table testutildate(id int primary key, d1 datetime, d2 timestamp)").executeUpdate();

        sql2o.createQuery("insert into testutildate(id, d1, d2) values(:id, :d1, :d2)")
                .addParameter("id", 1).addParameter("d1", new Date()).addParameter("d2", new Date()).addToBatch()
                .addParameter("id", 2).addParameter("d1", new Date()).addParameter("d2", new Date()).addToBatch()
                .addParameter("id", 3).addParameter("d1", new Date()).addParameter("d2", new Date()).addToBatch()
                .executeBatch();

        List<UtilDateEntity> list = sql2o.createQuery("select * from testutildate").executeAndFetch(UtilDateEntity.class);

        assertTrue(list.size() == 3);

    }

    public void testComplexTypes(){
        List<ComplexEntity> list = sql2o.createQuery("select 1 id, 'test' val, 'test2' \"obj.val1\", 2 \"obj.valint\" union select 2 id, 'test2' val, 'tessdf' \"obj.val1\", 100 \"obj.valint\"").executeAndFetch(ComplexEntity.class);

        assertTrue(list.size() == 2);
        //assertEquals(2, list.get(0).obj.valInt);
    }

    public void testConversion(){

        String sql = "select cast(1 as smallint) as val1, 2 as val2 union select cast(3 as smallint) as val1, 4 as val2";
        List<TypeConvertEntity> entities = sql2o.createQuery(sql).executeAndFetch(TypeConvertEntity.class);

        assertTrue(entities.size() == 2);
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
        insQuery.executeBatch();
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
