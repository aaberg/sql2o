package org.sql2o;

import com.mysql.jdbc.Driver;
import junit.framework.TestCase;
import sun.rmi.log.LogInputStream;

import java.io.Console;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Test extends TestCase {

    public void testWithH2() throws SQLException {

        Sql2o.registerDriver(new org.h2.Driver());

        Sql2o.registerDriver(new com.mysql.jdbc.Driver());


        String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        //String url = "jdbc:h2:file:~/db/test";

        Sql2o sql2o = new Sql2o(url, "sa", "");

        HashMap<String, String> colMaps = new HashMap<String,String>();
        colMaps.put("ID", "id");
        colMaps.put("NAME", "name");
        colMaps.put("EMAIL", "email");
        colMaps.put("TEXT", "text2");

        sql2o.setDefaultColumnMappings(colMaps);

        String createQuery =
                "create table User(" +
                        "id int identity primary key," +
                        "name varchar(20)," +
                        "email varchar(255)," +
                        "text varchar(100))";

        sql2o.createQuery(createQuery).executeUpdate();

        Date beginTime = new Date();
        
        int iterations = 10000;
        for (int idx = 0; idx < iterations; idx++){
            sql2o.createQuery("insert into User(name, email, text) values(:name, :email, :text)")
                    .addParameter("name", "testnavn" + idx)
                    .addParameter("text", "ipsum lupsim " + idx)
                    .addParameter("email", "test@email" + idx + ".com")
                    .executeUpdate();
        }

        System.out.println(String.format("inserted %s rows with no transaction: %s milliseconds", iterations, new Date().getTime() - beginTime.getTime()));

        int iterations2 = 10000;
        beginTime = new Date();

        Query insertQuery = sql2o.createQuery("insert into User(name, email, text) values(:name, :email, :text)").beginTransaction();
        for (int idx = 0; idx < iterations2; idx++){
            insertQuery
                    .addParameter("name", "testnavn" + idx)
                    .addParameter("text", "ipsum lupsim " + idx)
                    .addParameter("email", "test@email" + idx + ".com")
                    .executeUpdate();
        }
        insertQuery.commit();
        System.out.println(String.format("inserted %s rows in transaction and committed: %s milliseconds", iterations2, new Date().getTime() - beginTime.getTime()));


        int iterations3 = 10000;
        beginTime = new Date();

        Query insertQueryToRollback = sql2o.createQuery("insert into User(name, email, text) values(:name, :email, :text)").beginTransaction();
        for (int idx = 0; idx < iterations2; idx++){
            insertQueryToRollback
                    .addParameter("name", "testnavn" + idx)
                    .addParameter("text", "ipsum lupsim " + idx)
                    .addParameter("email", "test@email" + idx + ".com")
                    .executeUpdate();
        }
        insertQueryToRollback.rollback();
        System.out.println(String.format("inserted %s rows in transaction and rolled back: %s milliseconds", iterations3, new Date().getTime() - beginTime.getTime()));

        String selectQuery =
                "select id, name, email, text\n" +
                "from user";

        Date startTime = new Date();

        List<User> allUsers = sql2o.createQuery(selectQuery)
                .addColumnMapping("TEXT", "text")
                .executeAndFetch(User.class);

        Date endTime = new Date();

        Long ellapsedMilliseconds = endTime.getTime() - startTime.getTime();
        System.out.println(String.format("fetch time for %s rows: %s", allUsers.size(), ellapsedMilliseconds));

        assertTrue(allUsers.size() == iterations + iterations2);
    }
}
