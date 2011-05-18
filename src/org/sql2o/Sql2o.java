package org.sql2o;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sql2o {

    public Sql2o(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    private String url;
    private String user;
    private String pass;

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public Query createQuery(String query, Class c){
        Query q = new Query(this, c, query);

        return q;
    }




    public static void registerDriver(String driverName){

        try{
            Driver driver = (Driver) Class.forName(driverName).newInstance();
            DriverManager.registerDriver(driver);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

    }
}
