package org.sql2o;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 10/12/11
 * Time: 10:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Connection {

    private java.sql.Connection jdbcConnection;
    private Sql2o sql2o;

    public Connection(Sql2o sql2o) {

        this.sql2o = sql2o;
        createConnection();
    }

    public java.sql.Connection getJdbcConnection() {
        return jdbcConnection;
    }

    public Sql2o getSql2o() {
        return sql2o;
    }

    public Query createQuery(String queryText){

        try {
            if (this.getJdbcConnection().isClosed()){
                createConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Query q = new Query(this, queryText);
        return q;
    }


    public Sql2o rollback(){
        try {
            this.getJdbcConnection().rollback();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                this.getJdbcConnection().close();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return this.getSql2o();
    }

    public Sql2o commit(){
        try {
            this.getJdbcConnection().commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                this.getJdbcConnection().close();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return this.getSql2o();
    }

    private void createConnection(){
        Properties conProps = new Properties();
        conProps.put("user", sql2o.getUser());
        conProps.put("password", sql2o.getPass());

        String url = this.sql2o.getUrl();
        try{

            if (!url.startsWith("jdbc")){
                url = "jdbc:" + url;
            }
            this.jdbcConnection = DriverManager.getConnection(url, conProps);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }


}
