package org.sql2o;

import org.omg.CORBA.PUBLIC_MEMBER;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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

        this.defaultColumnMappings = new HashMap<String, String>();
    }

    private String url;
    private String user;
    private String pass;

    private Map<String, String> defaultColumnMappings;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public Map<String, String> getDefaultColumnMappings() {
        return defaultColumnMappings;
    }

    public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
        this.defaultColumnMappings = defaultColumnMappings;
    }

    public Query createQuery(String query){
        Query q = new Query(this, query, defaultColumnMappings);

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

    public static void registerDriver(Driver driver){
        try {
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
