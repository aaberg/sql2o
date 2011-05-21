package org.sql2o;

import junit.framework.TestCase;

import javax.sound.midi.SysexMessage;
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
    }


    /************** Helper stuff ******************/

    private void createAndFillUserTable(){
        sql2o.createQuery(
                "create table User(\n" +
                "id int identity primary key,\n" +
                "name varchar(20),\n" +
                "email varchar(255),\n" +
                "text varchar(100))").executeUpdate();

        Query insQuery = sql2o.beginTransaction().createQuery("insert into User(name, email, text) values (:name, :email, :text)");
        Date before = new Date();
        for (int idx = 0; idx < 10000; idx++){
            insQuery.addParameter("name", "a name " + idx)
                    .addParameter("email", String.format("test%s@email.com", idx))
                    .addParameter("text", "some text").executeUpdate();
        }
        sql2o.commit();
        Date after = new Date();
        Long span = after.getTime() - before.getTime();

        System.out.println(String.format("inserted 10000 rows into User table. Time used: %s ms", span));

        insertIntoUsers += 10000;
    }

    private void createTestEntityTable(String tableName){
        
    }
}
