package org.sql2o;

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

        this.createConnection();
    }

    private String url;
    private String user;
    private String pass;

    private Connection connection;

    private Map<String, String> defaultColumnMappings;

    private boolean defaultCaseSensitive = false;

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

    public boolean isDefaultCaseSensitive() {
        return defaultCaseSensitive;
    }

    public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
        this.defaultCaseSensitive = defaultCaseSensitive;
    }

    public Connection getConnection() {
        return connection;
    }


    public Query createQuery(String query){

        try {
            if (this.getConnection().isClosed()){
                this.createConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Query q = new Query(this, query);

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

    private void createConnection(){

        Properties conProps = new Properties();
        conProps.put("user", this.getUser());
        conProps.put("password", this.getPass());
        try{
            if (!this.getUrl().startsWith("jdbc")){
                this.setUrl("jdbc:" + this.getUrl());
            }
            this.connection = DriverManager.getConnection(this.getUrl(), conProps);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

    }

    public Sql2o beginTransaction(int isolationLevel){

        try {
            if (this.connection.isClosed()){
                this.createConnection();
            }

            this.getConnection().setAutoCommit(false);
            this.getConnection().setTransactionIsolation(isolationLevel);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public Sql2o beginTransaction(){
        return this.beginTransaction(Connection.TRANSACTION_READ_COMMITTED);
    }

    public Sql2o commit(){
        try {
            this.getConnection().commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                this.getConnection().close();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public Sql2o rollback(){
        try {
            this.getConnection().rollback();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                this.getConnection().close();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }
}
