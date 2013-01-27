package org.sql2o;

    import javax.sql.DataSource;
    import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sql2o is the main class for the sql2o library.
 * <p>
 * An <code>Sql2o</code> instance represents a way of connecting to one specific database.
 * To create a new instance, one need to specify either jdbc-url, username and password for the database or a data source.
 * <p>
 * Internally the Sql2o instance uses a data source to create jdbc connections to the database. If url, username and password
 * was specified in the constructor, a simple data source is created, which works as a simple wrapper around the jdbc
 * driver.
 * <p>
 * Some jdbc implementations has quirks, therefor it may be necessary to use a constructor with the quirksMode parameter.
 * When quirksMode is specified, Sql2o will use workarounds to avoid these quirks.
 * @author Lars Aaberg
 */

public class Sql2o {

    /**
     * Creates a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url   JDBC database url
     * @param user  database username
     * @param pass  database password
     */
    public Sql2o(String url, String user, String pass){
        this(url,user,pass,QuirksMode.None);
    }

    /**
     * Created a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url   JDBC database url
     * @param user  database username
     * @param pass  database password
     * @param quirksMode    @{link QuirksMode} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(String url, String user, String pass, QuirksMode quirksMode) {
        this(new GenericDatasource(url, user, pass), quirksMode);
    }

    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource    The DataSource Sql2o uses to acquire connections to the database.
     */
    public Sql2o(DataSource dataSource) {
        this(dataSource, QuirksMode.None);
    }

    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource    The DataSource Sql2o uses to acquire connections to the database.
     * @param quirksMode    @{link QuirksMode} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(DataSource dataSource, QuirksMode quirksMode){
        this.dataSource = dataSource;
        this.quirksMode = quirksMode;

        this.defaultColumnMappings = new HashMap<String, String>();
    }

    private final DataSource dataSource;
    QuirksMode quirksMode;

    private Map<String, String> defaultColumnMappings;

    private boolean defaultCaseSensitive = false;

    /**
     * Gets the DataSource that Sql2o uses internally to acquire database connections.
     * @return  The DataSource instance
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Gets the default column mappings Map. column mappings added to this Map are always available when Sql2o attempts
     * to map between result sets and object instances.
     * @return  The {@link Map<String, String>} instance, which Sql2o internally uses to map column names with property
     * names.
     */
    public Map<String, String> getDefaultColumnMappings() {
        return defaultColumnMappings;
    }

    /**
     * Sets the default column mappings Map.
     * @param defaultColumnMappings     A {@link Map} instance Sql2o uses internally to map between column names and
     *                                  property names.
     */
    public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
        this.defaultColumnMappings = defaultColumnMappings;
    }

    /**
     * Gets value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names.
     * @return
     */
    public boolean isDefaultCaseSensitive() {
        return defaultCaseSensitive;
    }

    /**
     * Sets a value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names. This should almost always be false, because most relational databases are not case sensitive.
     * @param defaultCaseSensitive
     */
    public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
        this.defaultCaseSensitive = defaultCaseSensitive;
    }

    public Query createQuery(String query, String name, boolean returnGeneratedKeys) {
        return new Connection(this).createQuery(query, name, returnGeneratedKeys);
    }

    public Query createQuery(String query, boolean returnGeneratedKeys) {
        return createQuery(query, null, returnGeneratedKeys);
    }

    public Query createQuery(String query, String name){

        Connection connection = new Connection(this);
        return connection.createQuery(query, name);
    }
    
    public Query createQuery(String query){
        return createQuery(query, null);
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
        connection.setRollbackOnException(false);

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
