package org.sql2o;

import junit.framework.TestCase;

import java.sql.Driver;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Test extends TestCase {

    public void testSelect(){
        String driver = "com.mysql.jdbc.Driver";
        String url = "mysql://127.0.0.1/playblog";
        String user = "root";
        String pass = "test123";

        Sql2o.registerDriver(driver);
        Sql2o sql2o = new Sql2o(url, user, pass);


        String query =
                "select id, name, email, \n" +
                "\t'test' as \"customer1.name\", \n" +
                "\t1 as \"customer1.id\",\n" +
                "\t'test2' as \"customer2.name\",\n" +
                "\t2 as \"customer2.id\"\n" +
                "from User where id = :id";

        User user3 = sql2o.createQuery(query, User.class).addParameter("id", 2).fetchFirst();

        List<User> userO = sql2o.createQuery("select id, name, email from User where id = :id", User.class).addParameter("id", 2L).fetch();

        List<User> user1 = sql2o.createQuery("select id, name, email from User where id = :id", User.class).addParameter("id", 3L).fetch();

        List<User> user2 = sql2o.createQuery("select id, name, email from User", User.class).fetch();

        assertNotNull(userO);
    }
}
