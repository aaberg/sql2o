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

    public void runInTransaction(StatementRunnable runnable){
        runInTransaction(runnable, null);
    }

    public void runInTransaction(StatementRunnable runnable, Object argument){
        runInTransaction(runnable, argument, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    public void runInTransaction(StatementRunnable runnable, Object argument, int isolationLevel){

        Connection connection = this.beginTransaction(isolationLevel);

        try {
            runnable.run(connection, argument);
        } catch (Throwable throwable) {
            connection.rollback();
            throw new Sql2oException("An error occurred while executing StatementRunnable. Transaction is rolled back.", throwable);
        }
        connection.commit();
    }

    public <V> V runInTransaction(StatementRunnableWithResult runnableWithResult){
        return runInTransaction(runnableWithResult, null);
    }
    
    public <V> V runInTransaction(StatementRunnableWithResult runnableWithResult, Object argument){
        return runInTransaction(runnableWithResult, argument, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }
    
    public <V> V runInTransaction(StatementRunnableWithResult runnableWithResult, Object argument, int isolationLevel){
        Connection connection = this.beginTransaction(isolationLevel);
        Object result;
        
        try{
            result = runnableWithResult.run(connection, argument);
        } catch (Throwable throwable) {
            connection.rollback();
            throw new Sql2oException("An error occurred while executing StatementRunnableWithResult. Transaction rolled back.", throwable);
        }
        
        connection.commit();
        return (V)result;

    }

}
