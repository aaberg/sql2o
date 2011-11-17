package org.sql2o;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    private final String url;
    private final String user;
    private final String pass;

    //private Connection connection;

    private Map<String, String> defaultColumnMappings;

    private boolean defaultCaseSensitive = false;

    public String getUrl() {
        return url;
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

    public boolean isDefaultCaseSensitive() {
        return defaultCaseSensitive;
    }

    public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
        this.defaultCaseSensitive = defaultCaseSensitive;
    }

    public Query createQuery(String query){

        Connection connection = new Connection(this);
        return connection.createQuery(query);
    }

//    public static void registerDriver(String driverName){
//
//        try{
//            Driver driver = (Driver) Class.forName(driverName).newInstance();
//            DriverManager.registerDriver(driver);
//        }
//        catch(Exception ex){
//            throw new RuntimeException(ex);
//        }
//
//    }
//
//    public static void registerDriver(Driver driver){
//        try {
//            DriverManager.registerDriver(driver);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public Connection beginTransaction(int isolationLevel){

        Connection connection = new Connection(this);

        try {
            connection.getJdbcConnection().setAutoCommit(false);
            connection.getJdbcConnection().setTransactionIsolation(isolationLevel);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    public Connection beginTransaction(){
        return this.beginTransaction(java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }


}
