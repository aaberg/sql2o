package org.sql2o;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.sql2o.connectionsources.ConnectionSource;
import org.sql2o.connectionsources.DataSourceConnectionSource;
import org.sql2o.quirks.Quirks;
import org.sql2o.quirks.QuirksDetector;

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
 * Some jdbc implementations have quirks, therefore it may be necessary to use a constructor with the quirks parameter.
 * When quirks are specified, Sql2o will use workarounds to avoid these quirks.
 * @author Lars Aaberg
 */
public class Sql2o {
    final Quirks quirks;
    private Map<String, String> defaultColumnMappings;
    private boolean defaultCaseSensitive;

    private ConnectionSource connectionSource;
    
    private ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder;
    
    public Sql2o(String jndiLookup) {
        this(JndiDataSource.getJndiDatasource(jndiLookup), (ResultSetHandlerFactoryBuilder) null);
    }
    
    public Sql2o(String jndiLookup, ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder) {
        this(JndiDataSource.getJndiDatasource(jndiLookup), defaultResultSetHandlerFactoryBuilder);
    }
    
    /**
     * Creates a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url   JDBC database url
     * @param user  database username
     * @param pass  database password
     */
    public Sql2o(String url, String user, String pass, ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder) {
        this(url, user, pass, QuirksDetector.forURL(url), defaultResultSetHandlerFactoryBuilder);
    }
    
    public Sql2o(String url, String user, String pass) {
        this(url, user, pass, QuirksDetector.forURL(url), null);
    }

    /**
     * Created a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url    JDBC database url
     * @param user   database username
     * @param pass   database password
     * @param quirks {@link org.sql2o.quirks.Quirks} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(
        String url, 
        String user, 
        String pass, 
        Quirks quirks, 
        ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder
    ) {
        this(new GenericDatasource(url, user, pass), quirks, defaultResultSetHandlerFactoryBuilder);
    }
    
    public Sql2o(String url, String user, String pass, Quirks quirks) {
        this(new GenericDatasource(url, user, pass), quirks, null);
    }
    
    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource    The DataSource Sql2o uses to acquire connections to the database.
     */
    public Sql2o(DataSource dataSource, ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder) {
        this(dataSource, QuirksDetector.forObject(dataSource), defaultResultSetHandlerFactoryBuilder);
    }
    
    public Sql2o(DataSource dataSource) {
        this(dataSource, QuirksDetector.forObject(dataSource), null);
    }
    
    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource The DataSource Sql2o uses to acquire connections to the database.
     * @param quirks     {@link org.sql2o.quirks.Quirks} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(DataSource dataSource, Quirks quirks, ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder) {
        this.connectionSource = new DataSourceConnectionSource(dataSource);
        this.quirks=quirks;
        this.defaultColumnMappings = new HashMap<String, String>();
        this.defaultResultSetHandlerFactoryBuilder = defaultResultSetHandlerFactoryBuilder;
    }
    
    public Sql2o(DataSource dataSource, Quirks quirks) {
        this(dataSource, quirks, null);
    }
    
    public Quirks getQuirks() {
        return this.quirks;
    }

     /**
     * Gets the DataSource that Sql2o uses internally to acquire database connections.
     * @deprecated use {@link #getConnectionSource()} as more general connection provider
     * @return  The DataSource instance
     */
     @Deprecated
    public DataSource getDataSource() {
        if (this.connectionSource instanceof DataSourceConnectionSource)
            return ((DataSourceConnectionSource) this.connectionSource).getDataSource();
        return null;
    }

    /**
     * Gets the {@link ConnectionSource} that Sql2o uses internally to acquire database connections.
     * @return The ConnectionSource instance
     */
    public ConnectionSource getConnectionSource() {
        return this.connectionSource;
    }

    /**
     * Sets the {@link ConnectionSource} that Sql2o uses internally to acquire database connections.
     * @param connectionSource the ConnectionSource instance to use
     */
    public void setConnectionSource(ConnectionSource connectionSource) {
        this.connectionSource = connectionSource;
    }

    /**
     * Gets the default column mappings Map. column mappings added to this Map are always available when Sql2o attempts
     * to map between result sets and object instances.
     * @return  The {@link Map<String, String>} instance, which Sql2o internally uses to map column names with property
     * names.
     */
    public Map<String, String> getDefaultColumnMappings() {
        return this.defaultColumnMappings;
    }

    /**
     * Sets the default column mappings Map.
     * @param defaultColumnMappings     A {@link Map} instance Sql2o uses internally to map between column names and
     *                                  property names.
     */
    public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
        this.defaultColumnMappings = defaultColumnMappings;
    }
    
    public ResultSetHandlerFactoryBuilder getDefaultResultSetHandlerFactoryBuilder() {
        return this.defaultResultSetHandlerFactoryBuilder;
    }
    
    public void setDefaultResultSetHandlerFactoryBuilder(ResultSetHandlerFactoryBuilder defaultResultSetHandlerFactoryBuilder) {
        this.defaultResultSetHandlerFactoryBuilder = defaultResultSetHandlerFactoryBuilder;
    }
    
    /**
     * Gets value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names.
     * @return
     */
    public boolean isDefaultCaseSensitive() {
        return this.defaultCaseSensitive;
    }

    /**
     * Sets a value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names. This should almost always be false, because most relational databases are not case sensitive.
     * @param defaultCaseSensitive
     */
    public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
        this.defaultCaseSensitive = defaultCaseSensitive;
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @param returnGeneratedKeys boolean value indicating if the database should return any generated keys.
     * @return the {@link Query} instance
     *
     * @deprecated create queries with {@link org.sql2o.Connection} class instead, using try-with-resource blocks
     * <code>
     *     try (Connection con = sql2o.open()) {
     *         return sql2o.createQuery(query, name, returnGeneratedKeys).executeAndFetch(Pojo.class);
     *     }
     * </code>
     */
    @Deprecated
    public Query createQuery(String sql, boolean returnGeneratedKeys) {
        try (Connection con = new Connection(this, true); Query query = con.createQuery(sql, returnGeneratedKeys)) {
            return query;
        }
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @return the {@link Query} instance
     *
     * @deprecated create queries with {@link org.sql2o.Connection} class instead, using try-with-resource blocks
     * <code>
     *     try (Connection con = sql2o.open()) {
     *         return sql2o.createQuery(query, name).executeAndFetch(Pojo.class);
     *     }
     * </code>
     */
    @Deprecated
    public Query createQuery(String query){

        try (Connection connection = new Connection(this, true)) {
            return connection.createQuery(query);
        }
    }

    /**
     * Opens a connection to the database
     * @param connectionSource the {@link ConnectionSource} implementation substitution,
     *                         that will be used instead of one from {@link Sql2o} instance.
     * @return instance of the {@link org.sql2o.Connection} class.
     */
    public Connection open(ConnectionSource connectionSource) {
        return new Connection(this, connectionSource, false, this.defaultResultSetHandlerFactoryBuilder);
    }

    /**
     * Opens a connection to the database
     * @return instance of the {@link org.sql2o.Connection} class.
     */
    public Connection open() {
        return new Connection(this, false, this.defaultResultSetHandlerFactoryBuilder);
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param argument
     * @param <V>
     * @return
     */
    public <V> V withConnection(StatementRunnableWithResult<V> runnable, Object argument) {
        try (Connection connection = open()) {
            return runnable.run(connection, argument);
        } catch (Throwable t) {
            throw new Sql2oException("An error occurred while executing StatementRunnable", t);
        }
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param <V>
     * @return
     */
    public <V> V withConnection(StatementRunnableWithResult<V> runnable) {
        return withConnection(runnable, null);
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     */
    public void withConnection(StatementRunnable runnable) {
        withConnection(runnable, null);
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param argument
     */
    public void withConnection(StatementRunnable runnable, Object argument) {
        try (Connection connection = open()) {
            runnable.run(connection, argument);
        } catch (Throwable t) {
            throw new Sql2oException("An error occurred while executing StatementRunnable", t);
        }
    }

    /**
     * Begins a transaction with the given isolation level. Every statement executed on the return {@link Connection}
     * instance, will be executed in the transaction. It is very important to always call either the {@link org.sql2o.Connection#commit()}
     * method or the {@link org.sql2o.Connection#rollback()} method to close the transaction. Use proper try-catch logic.
     * @param isolationLevel the isolation level of the transaction
     * @return the {@link Connection} instance to use to run statements in the transaction.
     */
    public Connection beginTransaction(int isolationLevel){
        return beginTransaction(getConnectionSource(), isolationLevel);
    }

    /**
     * Begins a transaction with the given isolation level. Every statement executed on the return {@link Connection}
     * instance, will be executed in the transaction. It is very important to always call either the {@link org.sql2o.Connection#commit()}
     * method or the {@link org.sql2o.Connection#rollback()} method to close the transaction. Use proper try-catch logic.
     * @param connectionSource the {@link ConnectionSource} implementation substitution,
     *                         that will be used instead of one from {@link Sql2o} instance.
     * @param isolationLevel the isolation level of the transaction
     * @return the {@link Connection} instance to use to run statements in the transaction.
     */
    public Connection beginTransaction(ConnectionSource connectionSource, int isolationLevel) {

        Connection connection = new Connection(this, connectionSource, false, this.defaultResultSetHandlerFactoryBuilder);

        boolean success = false;
        try {
            connection.getJdbcConnection().setAutoCommit(false);
            connection.getJdbcConnection().setTransactionIsolation(isolationLevel);
            success = true;
        } catch (SQLException e) {
            throw new Sql2oException("Could not start the transaction - " + e.getMessage(), e);
        } finally {
            if (!success) {
                connection.close();
            }
        }

        return connection;
    }

    /**
     * Begins a transaction with isolation level {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}. Every statement executed on the return {@link Connection}
     * instance, will be executed in the transaction. It is very important to always call either the {@link org.sql2o.Connection#commit()}
     * method or the {@link org.sql2o.Connection#rollback()} method to close the transaction. Use proper try-catch logic.
     * @return the {@link Connection} instance to use to run statements in the transaction.
     */
    public Connection beginTransaction(){
        return this.beginTransaction(java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * Begins a transaction with isolation level {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}. Every statement executed on the return {@link Connection}
     * instance, will be executed in the transaction. It is very important to always call either the {@link org.sql2o.Connection#commit()}
     * method or the {@link org.sql2o.Connection#rollback()} method to close the transaction. Use proper try-catch logic.
     * @param connectionSource the {@link ConnectionSource} implementation substitution,
     *                         that will be used instead of one from {@link Sql2o} instance.
     * @return the {@link Connection} instance to use to run statements in the transaction.
     */
    public Connection beginTransaction(ConnectionSource connectionSource) {
        return this.beginTransaction(connectionSource, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * Calls the {@link StatementRunnable#run(Connection, Object)} method on the {@link StatementRunnable} parameter. All statements
     * run on the {@link Connection} instance in the {@link StatementRunnable#run(Connection, Object) run} method will be
     * executed in a transaction. The transaction will automatically be committed if the {@link StatementRunnable#run(Connection, Object) run}
     * method finishes without throwing an exception. If an exception is thrown within the {@link StatementRunnable#run(Connection, Object) run} method,
     * the transaction will automatically be rolled back.
     *
     * The isolation level of the transaction will be set to {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}
     * @param runnable The {@link StatementRunnable} instance.
     */
    public void runInTransaction(StatementRunnable runnable){
        runInTransaction(runnable, null);
    }

    /**
     * Calls the {@link StatementRunnable#run(Connection, Object)} method on the {@link StatementRunnable} parameter. All statements
     * run on the {@link Connection} instance in the {@link StatementRunnable#run(Connection, Object) run} method will be
     * executed in a transaction. The transaction will automatically be committed if the {@link StatementRunnable#run(Connection, Object) run}
     * method finishes without throwing an exception. If an exception is thrown within the {@link StatementRunnable#run(Connection, Object) run} method,
     * the transaction will automatically be rolled back.
     *
     * The isolation level of the transaction will be set to {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}
     * @param runnable The {@link StatementRunnable} instance.
     * @param argument An argument which will be forwarded to the {@link StatementRunnable#run(Connection, Object) run} method
     */
    public void runInTransaction(StatementRunnable runnable, Object argument){
        runInTransaction(runnable, argument, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * Calls the {@link StatementRunnable#run(Connection, Object)} method on the {@link StatementRunnable} parameter. All statements
     * run on the {@link Connection} instance in the {@link StatementRunnable#run(Connection, Object) run} method will be
     * executed in a transaction. The transaction will automatically be committed if the {@link StatementRunnable#run(Connection, Object) run}
     * method finishes without throwing an exception. If an exception is thrown within the {@link StatementRunnable#run(Connection, Object) run} method,
     * the transaction will automatically be rolled back.
     * @param runnable The {@link StatementRunnable} instance.
     * @param argument An argument which will be forwarded to the {@link StatementRunnable#run(Connection, Object) run} method
     * @param isolationLevel The isolation level of the transaction
     */
    public void runInTransaction(StatementRunnable runnable, Object argument, int isolationLevel){

        try (Connection connection = this.beginTransaction(isolationLevel)) {
            connection.setRollbackOnException(false);
            runnable.run(connection, argument);
            connection.commit();
        } catch (Throwable throwable) {
            throw new Sql2oException("An error occurred while executing StatementRunnable. Transaction is rolled back.", throwable);
        }
    }

    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult){
        return runInTransaction(runnableWithResult, null);
    }
    
    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument){
        return runInTransaction(runnableWithResult, argument, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    @SuppressWarnings("unchecked")
    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument, int isolationLevel){
        Object result;
        
        try (Connection connection = this.beginTransaction(isolationLevel)) {
            result = runnableWithResult.run(connection, argument);
            
            connection.commit();
            return (V)result;
        } catch (Throwable throwable) {
            throw new Sql2oException("An error occurred while executing StatementRunnableWithResult. Transaction rolled back.", throwable);
        }
    }

}
