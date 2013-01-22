package org.sql2o;

import junit.framework.TestCase;
import org.hsqldb.jdbcDriver;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sql2o.data.Row;
import org.sql2o.data.Table;
import org.sql2o.pojos.*;

import java.math.BigDecimal;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/21/11
 * Time: 9:25 PM
 * Most sql2o tests are in this class.
 */
@RunWith(Parameterized.class)
public class Sql2oTest {

    @Parameterized.Parameters(name = "{index} - {4}")
    public static Collection<Object[]> getData(){
        return Arrays.asList(new Object[][]{
                {null, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","sa", "", "H2 test" },
                {new jdbcDriver(), "jdbc:hsqldb:mem:testmemdb", "SA", "", "HyperSQL DB test"}
        });
    }



    private final Sql2o sql2o;// = new Sql2o(this.url, this.user, this.pass);
    private String url;
    private String user;
    private String pass;

    public Sql2oTest(Driver driverToRegister, String url, String user, String pass, String testName){

        if (driverToRegister != null) {
            try {
                DriverManager.registerDriver(driverToRegister);
            } catch (SQLException e) {
                throw new RuntimeException("could not register driver '" + driverToRegister.getClass().getName() + "'", e);
            }
        }

        this.sql2o = new Sql2o(url, user, pass);

        HashMap<String, String> defaultColumnMap = new HashMap<String,String>();
        defaultColumnMap.put("ID", "id");
        defaultColumnMap.put("NAME", "name");
        defaultColumnMap.put("EMAIL", "email");
        defaultColumnMap.put("TEXT", "text");
        defaultColumnMap.put("ANUMBER", "aNumber");
        defaultColumnMap.put("ALONGNUMBER", "aLongNumber");
        sql2o.setDefaultColumnMappings(defaultColumnMap);

            this.url = url;
            this.user = user;
            this.pass = pass;

            if ("HyperSQL DB test".equals( testName )) {
                sql2o.createQuery("set database sql syntax MSS true").executeUpdate();
            }
    }

    private int insertIntoUsers = 0;


    @Test
    public void testExecuteAndFetch(){
        createAndFillUserTable();

        Date before = new Date();
        List<User> allUsers = sql2o.createQuery("select * from User").executeAndFetch(User.class);
        Date after = new Date();
        long span = after.getTime() - before.getTime();
        System.out.println(String.format("Fetched %s user: %s ms", insertIntoUsers, span));

        // repeat this
        before = new Date();
        allUsers = sql2o.createQuery("select * from User").executeAndFetch(User.class);
        after = new Date();
        span = after.getTime() - before.getTime();
        System.out.println(String.format("Again Fetched %s user: %s ms", insertIntoUsers, span));

        assertTrue(allUsers.size() == insertIntoUsers);
        deleteUserTable();
    }

    @Test
    public void testExecuteAndFetchWithNulls(){
        String sql =
                "create table testExecWithNullsTbl (" +
                        "id int identity primary key, " +
                        "text varchar(255), " +
                        "aNumber int, " +
                        "aLongNumber bigint)";
        sql2o.createQuery(sql, "testExecuteAndFetchWithNulls").executeUpdate();


        Connection connection = sql2o.beginTransaction();
        Query insQuery = connection.createQuery("insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
        insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", (Integer)null).addParameter("lnum", 10L).executeUpdate();
        insQuery.addParameter("text", (String)null).addParameter("number", 21).addParameter("lnum", (Long)null).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
        insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
        connection.commit();

        List<Entity> fetched = sql2o.createQuery("select * from testExecWithNullsTbl").executeAndFetch(Entity.class);

        assertTrue(fetched.size() == 5);
        assertNull(fetched.get(2).text);
        assertNotNull(fetched.get(3).text);

        assertNull(fetched.get(1).aNumber);
        assertNotNull(fetched.get(2).aNumber);

        assertNull(fetched.get(2).aLongNumber);
        assertNotNull(fetched.get(3).aLongNumber);
    }

    @Test
    public void testBatch(){
        sql2o.createQuery(
                "create table User(\n" +
                "id int identity primary key,\n" +
                "name varchar(20),\n" +
                "email varchar(255),\n" +
                "text varchar(100))").executeUpdate();

        String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

        Connection con = sql2o.beginTransaction();
        int[] inserted = con.createQuery(insQuery).addParameter("name", "test").addParameter("email", "test@test.com").addParameter("text", "something exciting").addToBatch()
                .addParameter("name", "test2").addParameter("email", "test2@test.com").addParameter("text", "something exciting too").addToBatch()
                .addParameter("name", "test3").addParameter("email", "test3@test.com").addParameter("text", "blablabla").addToBatch()
                .executeBatch().getBatchResult();
        con.commit();

        assertEquals(3, inserted.length);
        for (int i : inserted){
            assertEquals(1, i);
        }

        deleteUserTable();
    }

    @Test
    public void testExecuteScalar(){
        createAndFillUserTable();

        Object o = sql2o.createQuery("select text from User where id = 2").executeScalar();
        assertTrue(o.getClass().equals(String.class));

        Object o2 = sql2o.createQuery("select 10").executeScalar();
        assertEquals(o2, 10);

        deleteUserTable();
    }

    @Test
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

    @Test
    public void testCaseInsensitive(){
        sql2o.createQuery("create table testCI(id2 int primary key, value2 varchar(20), sometext varchar(20), valwithgetter varchar(20))").executeUpdate();

        Query query = sql2o.createQuery("insert into testCI(id2, value2, sometext, valwithgetter) values(:id, :value, :someText, :valwithgetter)");
        for (int i = 0; i < 20; i++){
            query.addParameter("id", i).addParameter("value", "some text " + i).addParameter("someText", "whatever " + i).addParameter("valwithgetter", "spaz" + i).addToBatch();
        }
        query.executeBatch();

        List<CIEntity> ciEntities = sql2o.createQuery("select * from testCI").setCaseSensitive(false).executeAndFetch(CIEntity.class);

        assertTrue(ciEntities.size() == 20);


        // test defaultCaseSensitive;
        sql2o.setDefaultCaseSensitive(false);
        List<CIEntity> ciEntities2 = sql2o.createQuery("select * from testCI").executeAndFetch(CIEntity.class);
        assertTrue(ciEntities2.size() == 20);
    }

    @Test
    public void testExecuteAndFetchResultSet() throws SQLException {
        List<Integer> list = sql2o.createQuery("select 1 val from (values(0)) union select 2 from (values(0)) union select 3 from (values(0))").executeScalarList();

        assertEquals((int)list.get(0), 1);
        assertEquals((int)list.get(1), 2);
        assertEquals((int)list.get(2), 3);

    }

    @Test
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

    @Test
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

    @Test
    public void testConversion(){

        String sql = "select cast(1 as smallint) as val1, 2 as val2 from (values(0)) union select cast(3 as smallint) as val1, 4 as val2 from (values(0))";
        List<TypeConvertEntity> entities = sql2o.createQuery(sql).executeAndFetch(TypeConvertEntity.class);

        assertTrue(entities.size() == 2);
    }

    @Test
    public void testUpdateNoTransaction() throws SQLException {
        String ddlQuery = "create table testUpdateNoTransaction(id int primary key, value varchar(50))";
        Connection connection = sql2o.createQuery(ddlQuery).executeUpdate();

        assertTrue(connection.getJdbcConnection().isClosed());

        String insQuery = "insert into testUpdateNoTransaction(id, value) values (:id, :value)";
        sql2o.createQuery(insQuery).addParameter("id",1).addParameter("value", "test1").executeUpdate()
                .createQuery(insQuery).addParameter("id", 2).addParameter("value","val2").executeUpdate();

        assertTrue(connection.getJdbcConnection().isClosed());
    }

    @Test
    public void testNullDate(){
        sql2o.createQuery("create table nullDateTest(id integer primary key, somedate datetime)").executeUpdate();

        sql2o.createQuery("insert into nullDateTest(id, somedate) values(:id, :date)")
                .addParameter("id", 1)
                .addParameter("date", (Date)null).executeUpdate();

        Date d = (Date)sql2o.createQuery("select somedate from nullDateTest where id = 1").executeScalar();
        assertNull(d);
    }

    @Test
    public void testGetResult(){

        sql2o.createQuery("create table get_result_test(id integer primary key, value varchar(20))").executeUpdate();

        String insertSql =
                "insert into get_result_test(id, value) " +
                "select 1, 'hello' from (values(0)) union " +
                "select 2, 'hello2' from (values(0)) union " +
                "select 3, 'hello3' from (values(0))";

        int result = sql2o.createQuery(insertSql).executeUpdate().getResult();

        assertEquals(3, result);
    }

    @Test
    public void testGetKeys(){

        sql2o.createQuery("create table get_keys_test(id integer identity primary key, value varchar(20))").executeUpdate();

        String insertSql = "insert into get_keys_test(value) values(:val)";
//        try{
//            Integer key = (Integer)sql2o.createQuery(insertSql).addParameter("val", "something").executeUpdate().getKey();
//            throw new RuntimeException("Sql2oException expected in code line above");
//        }
//        catch(Sql2oException ex){
//            assertTrue(ex.getMessage().contains("executeUpdate(true)"));
//        }

        Integer key = (Integer)sql2o.createQuery(insertSql).addParameter("val", "something").executeUpdate().getKey();

        assertNotNull(key);
        assertTrue(key >= 0);

        String multiInsertSql = "insert into get_keys_test(value) select 'a val' col1 from (values(0)) union select 'another val' col1 from (values(0))";
        Object[] keys = sql2o.createQuery(multiInsertSql).executeUpdate().getKeys();

        assertNotNull(keys);
        assertTrue(keys.length > 0);

        //The test below fails. H2 will always just return the identity of the last generated column.
        //todo: test with another database.
        //assertTrue(keys.length == 2);
    }

    @Test
    public void testRollback(){

        sql2o.createQuery("create table test_rollback_table(id integer identity primary key, value varchar(25))").executeUpdate();

        sql2o
                //first insert something, and commit it.
                .beginTransaction()
                .createQuery("insert into test_rollback_table(value) values (:val)")
                .addParameter("val", "something")
                .executeUpdate()
                .commit()

                // insert something else, and roll it back.
                .beginTransaction()
                .createQuery("insert into test_rollback_table(value) values (:val)")
                .addParameter("val", "something to rollback")
                .executeUpdate()
                .rollback();
        long rowCount = (Long)sql2o.createQuery("select count(*) from test_rollback_table").executeScalar();

        assertEquals(1, rowCount);
    }

    @Test
    public void testBigDecimals(){

        sql2o.createQuery("create table bigdectesttable (id integer identity primary key, val1 numeric(5,3), val2 integer)").executeUpdate();
        
        sql2o.createQuery("insert into bigdectesttable(val1, val2) values(:val1, :val2)").addParameter("val1",1.256).addParameter("val2", 4).executeUpdate();

        BigDecimalPojo pojo = sql2o.createQuery("select * from bigdectesttable").executeAndFetchFirst(BigDecimalPojo.class);

        assertEquals(new BigDecimal("1.256"), pojo.val1);
        assertEquals(new BigDecimal("4.0"), pojo.val2);
    }

    @Test
    public void testQueryDbMappings(){
        Entity entity = sql2o.createQuery("select 1 as id, 'something' as caption, cast('2011-01-01' as date) as theTime from (values(0))")
                .addColumnMapping("caption", "text")
                .addColumnMapping("theTime", "time").executeAndFetchFirst(Entity.class);

        assertEquals(1, entity.id);
        assertEquals("something", entity.text);
        assertEquals(new DateTime(2011,1,1,0,0,0,0).toDate(), entity.time);
    }

    @Test
    public void testGlobalDbMappings(){
        Sql2o sql2o1 = new Sql2o(url, user, pass);

        Map<String,String> defaultColMaps = new HashMap<String, String>();
        defaultColMaps.put("caption", "text");
        defaultColMaps.put("theTime", "time");

        sql2o1.setDefaultColumnMappings(defaultColMaps);

        Entity entity = sql2o1.createQuery("select 1 as id, 'something' as caption, cast('2011-01-01' as date) as theTime from (values(0))").executeAndFetchFirst(Entity.class);

        assertEquals(1, entity.id);
        assertEquals("something", entity.text);
        assertEquals(new DateTime(2011,1,1,0,0,0,0).toDate(), entity.time);

    }

    @Test
    public void testSetPrivateFields(){
        EntityWithPrivateFields entity = sql2o.createQuery("select 1 id, 'hello' value from (values(0))").executeAndFetchFirst(EntityWithPrivateFields.class);

        assertEquals(1, entity.getId());
        assertEquals("hello1", entity.getValue());
    }

    @Test
    public void testFetchTable(){
        sql2o.createQuery("create table tabletest(id integer identity primary key, value varchar(20), value2 decimal(5,1))").executeUpdate();
        sql2o.createQuery("insert into tabletest(value,value2) values (:value, :value2)")
                .addParameter("value", "something").addParameter("value2", new BigDecimal("3.4")).addToBatch()
                .addParameter("value", "bla").addParameter("value2", new BigDecimal("5.5")).addToBatch().executeBatch();


        Table table = sql2o.createQuery("select * from tabletest order by id").executeAndFetchTable();

        assertEquals(3, table.columns().size());
        assertEquals("ID", table.columns().get(0).getName());
        assertEquals("VALUE", table.columns().get(1).getName());
        assertEquals("VALUE2", table.columns().get(2).getName());

        assertEquals(2, table.rows().size());

        Row row0 = table.rows().get(0);
        Row row1 = table.rows().get(1);

        assertTrue(0 <= row0.getInteger("ID"));
        assertEquals("something", row0.getString(1));
        assertEquals(new BigDecimal("3.4"), row0.getBigDecimal("VALUE2"));
        
        assertTrue(1 <= row1.getInteger(0));
        assertEquals("bla", row1.getString("VALUE"));
        assertEquals(5.5D, row1.getDouble(2), 0.00001);
    }

    @Test
    public void testStringConversion(){
        StringConversionPojo pojo = sql2o.createQuery("select '1' val1, '2  ' val2, '' val3, '' val4, null val5 from (values(0))").executeAndFetchFirst(StringConversionPojo.class);

        assertEquals((Integer)1, pojo.val1);
        assertEquals(2l, pojo.val2);
        assertNull(pojo.val3);
        assertEquals(0, pojo.val4);
        assertNull(pojo.val5);


    }

    @Test
    public void testSuperPojo(){
        SuperPojo pojo = sql2o.createQuery("select 1 id, 'something' value from (values(0))").executeAndFetchFirst(SuperPojo.class);

        assertEquals(1, pojo.getId());
        assertEquals("something1", pojo.getValue());
    }

    @Test
    public void testComplexTypes(){
        ComplexEntity pojo = sql2o.createQuery("select 1 id, 1 \"entity.id\", 'something' \"entity.value\" from (values(0))", "testComplexTypes").executeAndFetchFirst(ComplexEntity.class);

        assertEquals(1, pojo.id);
        assertEquals(1, pojo.entity.getId());
        assertEquals("something1", pojo.entity.getValue());
    }
    
//    public void testMultiResult(){
//        sql2o.createQuery("create table multi1(id integer identity primary key, value varchar(20))").executeUpdate();
//        sql2o.createQuery("create table multi2(id integer identity primary key, value2 varchar(20))").executeUpdate();
//
//        sql2o.createQuery("insert into multi1(value) values (:val)")
//                .addParameter("val", "test1").addToBatch()
//                .addParameter("val", "test2").addToBatch()
//                .executeBatch();
//
//        sql2o.createQuery("insert into multi2(value2) values (:val)")
//                .addParameter("val", "test3").addToBatch()
//                .addParameter("val", "test4").addToBatch()
//                .executeBatch();
//
//        List[] results = sql2o.createQuery("select * from multi1 order by id; select * from multi2 order by id").executeAndFetchMultiple(Multi1.class, Multi2.class);
//        //List<Multi1> results = sql2o.createQuery("select * from multi1 order by id; select * from multi2 order by id").executeAndFetch(Multi1.class);
//
//        List<Multi1> res1 = results[0];
//        List<Multi2> res2 = results[1];
//
//        assertEquals((Long)1L, res1.get(0).getId());
//        assertEquals("test2", res1.get(1).getValue());
//
//        assertEquals("test3", res2.get(0).getValue2());
//        assertEquals(4, res2.get(1).getId());
//    }

    @Test
    public void testRunInsideTransaction(){

        sql2o.createQuery("create table runinsidetransactiontable(id integer identity primary key, value varchar(50))").executeUpdate();
        boolean failed = false;

        try{
            sql2o.runInTransaction(new StatementRunnable() {
                public void run(Connection connection, Object argument) throws Throwable {
                    connection.createQuery("insert into runinsidetransactiontable(value) values(:value)")
                        .addParameter("value", "test").executeUpdate();
                    
                    throw new RuntimeException("ouch!");
    
    
                }
            });
        }
        catch(Sql2oException ex){
            failed = true;
        }
        
        assertTrue(failed);
        long rowCount = (Long)sql2o.createQuery("select count(*) from runinsidetransactiontable").executeScalar();
        assertEquals(0, rowCount);

        sql2o.runInTransaction(new StatementRunnable() {
            public void run(Connection connection, Object argument) throws Throwable {
                connection.createQuery("insert into runinsidetransactiontable(value) values(:value)")
                    .addParameter("value", "test").executeUpdate();
            }
        });

        rowCount = (Long)sql2o.createQuery("select count(*) from runinsidetransactiontable").executeScalar();
        assertEquals(1, rowCount);


        String argument = "argument test";
        
        sql2o.runInTransaction(new StatementRunnable() {
            public void run(Connection connection, Object argument) throws Throwable {
                Integer id = connection.createQuery("insert into runinsidetransactiontable(value) values(:value)")
                    .addParameter("value", argument).executeUpdate().getKey(Integer.class);
                
                String insertedValue = connection.createQuery("select value from runinsidetransactiontable where id = :id").addParameter("id", id).executeScalar(String.class);
                assertEquals("argument test", insertedValue);
            }
        }, argument);
    
        rowCount = (Long)sql2o.createQuery("select count(*) from runinsidetransactiontable").executeScalar();
        assertEquals(2, rowCount);
    }

    @Test
    public void testRunInsideTransactionWithResult(){
        sql2o.createQuery("create table testRunInsideTransactionWithResultTable(id integer identity primary key, value varchar(50))").executeUpdate();
        
    }
    
    private static class runnerWithResultTester implements StatementRunnableWithResult{

        public Object run(Connection connection, Object argument) throws Throwable {
            String[] vals = (String[])argument;
            List<Integer> keys = new ArrayList<Integer>();
            for (String val : vals){
                Integer key = connection.createQuery("insert into testRunInsideTransactionWithResultTable(value) values(:val)", "runnerWithResultTester")
                    .addParameter("val", val)
                    .executeUpdate().getKey(Integer.class);
                keys.add(key);
            }

            return keys;
        }
    }

    @Test
    public void testDynamicExecuteScalar(){
        Object origVal = sql2o.createQuery("select 1").executeScalar();
        assertTrue(Integer.class.equals(origVal.getClass()));
        assertEquals((Integer)1, origVal);
        
        Long intVal = sql2o.createQuery("select 1").executeScalar(Long.class);
        assertEquals((Long)1l, intVal);

        Short shortVal = sql2o.createQuery("select 2").executeScalar(Short.class);
        Short expected = 2;
        assertEquals(expected, shortVal);
    }

    @Test
    public void testUpdateWithNulls() {
        sql2o.createQuery("create table testUpdateWithNulls_2(id integer identity primary key, value integer)").executeUpdate();
        
        Integer nullInt = null;
        
        sql2o.createQuery("insert into testUpdateWithNulls_2(value) values(:val)").addParameter("val", 2).addToBatch().addParameter("val", nullInt).addToBatch().executeBatch();
    }

    @Test
    public void testExceptionInRunnable() {
        sql2o.createQuery("create table testExceptionInRunnable(id integer primary key, value varchar(20))").executeUpdate();
        
        try{
            sql2o.runInTransaction(new StatementRunnable() {
                public void run(Connection connection, Object argument) throws Throwable {
                    connection.createQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                        .addParameter("id", 1)
                        .addParameter("val", "something").executeUpdate();
                    
                    connection.createQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                        .addParameter("id", 1)
                        .addParameter("val", "something").executeUpdate();
                }
            });
        } catch(Throwable t) {
            
        }
        
        int c = (Integer)sql2o.createQuery("select count(*) from testExceptionInRunnable").executeScalar(Integer.class);
        assertEquals(0, c);


        sql2o.runInTransaction(new StatementRunnable() {
            public void run(Connection connection, Object argument) throws Throwable {
                connection.createQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                    .addParameter("id", 1)
                    .addParameter("val", "something").executeUpdate();

                try{
                    connection.createQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                        .addParameter("id", 1)
                        .addParameter("val", "something").executeUpdate();
                } catch(Sql2oException ex){

                }
            }
        });

        c = (Integer)sql2o.createQuery("select count(*) from testExceptionInRunnable").executeScalar(Integer.class);
        assertEquals(1, c);

    }

    public static enum TestEnum{
        HELLO, WORLD;
    }

    public static class EntityWithEnum{
        public int id;
        public TestEnum val;
        public TestEnum val2;
    }

    @Test
    public void testEnums() {
        sql2o.createQuery( "create table EnumTest(id int identity primary key, enum_val varchar(10), enum_val2 int) ").executeUpdate();

        sql2o.createQuery("insert into EnumTest(enum_val, enum_val2) values (:val, :val2)")
                .addParameter("val", TestEnum.HELLO).addParameter("val2", TestEnum.HELLO.ordinal()).addToBatch()
                .addParameter("val", TestEnum.WORLD).addParameter("val2", TestEnum.WORLD.ordinal()).addToBatch().executeBatch();

        List<EntityWithEnum> list = sql2o.createQuery("select id, enum_val val, enum_val2 val2 from EnumTest").executeAndFetch(EntityWithEnum.class);

        assertThat(list.get(0).val, is(TestEnum.HELLO));
        assertThat(list.get(0).val2, is(TestEnum.HELLO));
        assertThat(list.get(1).val, is(TestEnum.WORLD));
        assertThat(list.get(1).val2, is(TestEnum.WORLD));

        TestEnum testEnum = sql2o.createQuery("select 'HELLO' from (values(0))").executeScalar(TestEnum.class);
        assertThat(testEnum, is(TestEnum.HELLO));
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
