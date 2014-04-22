package org.sql2o;

import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;
import org.sql2o.tools.DefaultSqlParameterParsingStrategy;
import org.sql2o.tools.SqlParameterParsingStrategy;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
 * Some jdbc implementations have quirks, therefore it may be necessary to use a constructor with the quirks parameter.
 * When quirks are specified, Sql2o will use workarounds to avoid these quirks.
 * @author Lars Aaberg
 */
public class Sql2o {

    private Quirks quirks;
    private final DataSource dataSource;
    private Map<String, String> defaultColumnMappings;
    private boolean defaultCaseSensitive;
    private SqlParameterParsingStrategy sqlParameterParsingStrategy = new DefaultSqlParameterParsingStrategy();

    public Sql2o(String jndiLookup) {
        this(_getJndiDatasource(jndiLookup));
    }

    private static DataSource _getJndiDatasource(String jndiLookup) {
        Context ctx = null;
        DataSource datasource = null;

        try {
            ctx = new InitialContext();
            datasource = (DataSource) ctx.lookup(jndiLookup);
        }
        catch (NamingException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (ctx != null) {
                try {
                    ctx.close();
                }
                catch (NamingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return datasource;
    }

    /**
     * Creates a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url   JDBC database url
     * @param user  database username
     * @param pass  database password
     */
    public Sql2o(String url, String user, String pass){
        this(url, user, pass, new NoQuirks());
    }

    /**
     * Use {@link #Sql2o(String, String, String, org.sql2o.quirks.Quirks)}.
     */
    @Deprecated
    public Sql2o(String url, String user, String pass, QuirksMode quirksMode) {
        this(new GenericDatasource(url, user, pass), quirksMode);
    }

    /**
     * Created a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url    JDBC database url
     * @param user   database username
     * @param pass   database password
     * @param quirks {@link org.sql2o.quirks.Quirks} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(String url, String user, String pass, Quirks quirks) {
        this(new GenericDatasource(url, user, pass), quirks);
    }

    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource    The DataSource Sql2o uses to acquire connections to the database.
     */
    public Sql2o(DataSource dataSource) {
        this(dataSource, new NoQuirks());
    }

    /**
     * Use {@link #Sql2o(javax.sql.DataSource, org.sql2o.quirks.Quirks)}.
     */
    @Deprecated
    public Sql2o(DataSource dataSource, QuirksMode quirksMode) {
        this.dataSource = dataSource;
        this.setQuirks(quirksMode);
        this.defaultColumnMappings = new HashMap<String, String>();
    }

    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource The DataSource Sql2o uses to acquire connections to the database.
     * @param quirks     {@link org.sql2o.quirks.Quirks} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(DataSource dataSource, Quirks quirks){
        this.dataSource = dataSource;
        this.setQuirks(quirks);
        this.defaultColumnMappings = new HashMap<String, String>();
    }

    Quirks getQuirks() {
        return quirks;
    }

    private void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

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

    public SqlParameterParsingStrategy getSqlParameterParsingStrategy() {
        return sqlParameterParsingStrategy;
    }

    public void setSqlParameterParsingStrategy(SqlParameterParsingStrategy sqlParameterParsingStrategy) {
        this.sqlParameterParsingStrategy = sqlParameterParsingStrategy;
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @param name name of query. Only used for logging purposes
     * @param returnGeneratedKeys boolean value indicating if the database should return any generated keys.
     * @return the {@link Query} instance
     */
    public Query createQuery(String query, String name, boolean returnGeneratedKeys) {
        return new Connection(this, true).createQuery(query, name, returnGeneratedKeys);
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @param returnGeneratedKeys boolean value indicating if the database should return any generated keys.
     * @return the {@link Query} instance
     */
    public Query createQuery(String query, boolean returnGeneratedKeys) {
        return createQuery(query, null, returnGeneratedKeys);
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @param name name of query. Only used for logging purposes
     * @return the {@link Query} instance
     */
    public Query createQuery(String query, String name){

        Connection connection = new Connection(this, true);
        return connection.createQuery(query, name);
    }

    /**
     * Creates a {@link Query}
     * @param query the sql query string
     * @return the {@link Query} instance
     */
    public Query createQuery(String query){
        return createQuery(query, null);
    }

    public Query createProcedureCall(String commandText) {
        return new Connection(this, true).createProcedureCall(commandText);
    }

    public Query createProcedureCall(String commandText, String name) {
        return new Connection(this, true).createProcedureCall(commandText, name);
    }

    /**
     * Opens a connection to the database
     * @return instance of the {@link org.sql2o.Connection} class.
     */
    public Connection open() {
        return new Connection(this, false);
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param argument
     * @param <V>
     * @return
     */
    public <V> V withConnection(StatementRunnableWithResult runnable, Object argument) {
        Connection connection = null;
        try{
            connection = open();
            return (V)runnable.run(connection, argument);
        } catch (Throwable t) {
            throw new Sql2oException("An error occurred while executing StatementRunnable", t);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param <V>
     * @return
     */
    public <V> V withConnection(StatementRunnableWithResult runnable) {
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
        Connection connection = null;
        try{
            connection = open();

            runnable.run(connection, argument);
        } catch (Throwable t) {
            throw new Sql2oException("An error occurred while executing StatementRunnable", t);
        } finally{
            if (connection != null) {
                connection.close();
            }
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

        Connection connection = new Connection(this, false);

        try {
            connection.getJdbcConnection().setAutoCommit(false);
            connection.getJdbcConnection().setTransactionIsolation(isolationLevel);
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
