package org.sql2o;

import org.sql2o.connectionsources.DataSourceConnectionSource;
import org.sql2o.connectionsources.ConnectionSource;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;
import org.sql2o.quirks.Quirks;
import org.sql2o.quirks.QuirksDetector;

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

    private Settings settings;

    private ConnectionSource connectionSource;

    private final static Logger logger = LocalLoggerFactory.getLogger(Sql2o.class);

    public Sql2o(String jndiLookup) {
        this(JndiDataSource.getJndiDatasource(jndiLookup));
    }

    /**
     * Creates a new instance of the Sql2o class. Internally this constructor will create a {@link GenericDatasource},
     * and call the {@link Sql2o#Sql2o(javax.sql.DataSource)} constructor which takes a DataSource as parameter.
     * @param url   JDBC database url
     * @param user  database username
     * @param pass  database password
     */
    public Sql2o(String url, String user, String pass){
        this(url, user, pass, QuirksDetector.forURL(url));
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
        this(dataSource, QuirksDetector.forObject(dataSource));
    }

    /**
     * Creates a new instance of the Sql2o class, which uses the given DataSource to acquire connections to the database.
     * @param dataSource The DataSource Sql2o uses to acquire connections to the database.
     * @param quirks     {@link org.sql2o.quirks.Quirks} allows sql2o to work around known quirks and issues in different JDBC drivers.
     */
    public Sql2o(DataSource dataSource, Quirks quirks){
        this(new Settings(quirks, new HashMap<String, String>(), false));
        this.connectionSource = new DataSourceConnectionSource(dataSource);
    }

    /**
     * Creates a new instance of the Sql2o class without any {@link ConnectionSource}. Instance will be able only to
     * {@link Sql2o#wrap(java.sql.Connection) wrap} existing jdbc connections or work with explicitly passed {@link ConnectionSource}
     * with {@link Sql2o#open(ConnectionSource)} or {@link Sql2o#beginTransaction(ConnectionSource)} methods.
     *
     * @param settings Sql2o settings to be used in wrapped connections
     */
    public Sql2o(Settings settings) {
        this.settings = settings;
    }

    /**
     * Creates a new instance of the Sql2o class without any {@link ConnectionSource}. Instance will be able only to
     * {@link Sql2o#wrap(java.sql.Connection) wrap} existing jdbc connections or work with explicitly passed {@link ConnectionSource}
     * with {@link Sql2o#open(ConnectionSource)} or {@link Sql2o#beginTransaction(ConnectionSource)} methods.
     *
     * @param quirks quirks to be used in wrapped connections
     */
    public Sql2o(Quirks quirks) {
        this(Settings.defaults.withQuirks(quirks));
    }

    /**
     * Creates a new instance of the Sql2o class with default settings and without any {@link ConnectionSource}. Instance will be able only to
     * {@link Sql2o#wrap(java.sql.Connection) wrap} existing jdbc connections or work with explicitly passed {@link ConnectionSource}
     * with {@link Sql2o#open(ConnectionSource)} or {@link Sql2o#beginTransaction(ConnectionSource)} methods.
     */
    public Sql2o() {
        this(Settings.defaults);
    }

    /**
     * Gets the DataSource that Sql2o uses internally to acquire database connections.
     * @deprecated use {@link #getConnectionSource()} as more general connection provider
     * @return The DataSource instance
     */
    @Deprecated
    public DataSource getDataSource() {
        if (connectionSource instanceof DataSourceConnectionSource)
            return ((DataSourceConnectionSource) connectionSource).getDataSource();
        else
            return null;
    }

    /**
     * Gets the {@link ConnectionSource} that Sql2o uses internally to acquire database connections.
     * @return The ConnectionSource instance
     */
    public ConnectionSource getConnectionSource() {
        return connectionSource;
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
     * @deprecated use {@link Sql2o#getSettings()} instead
     * names.
     */
    @Deprecated
    public Map<String, String> getDefaultColumnMappings() {
        return settings.getDefaultColumnMappings();
    }

    /**
     * Sets the default column mappings Map.
     * @param defaultColumnMappings     A {@link Map} instance Sql2o uses internally to map between column names and
     *                                  property names.
     * @deprecated use {@link Sql2o#setSettings(Settings)} instead
     */
    @Deprecated
    public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
        this.settings = this.settings.withDefaultColumnMappings(defaultColumnMappings);
    }

    /**
     * Gets value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names.
     * @deprecated use {@link Sql2o#getSettings()} instead
     * @return
     */
    @Deprecated
    public boolean isDefaultCaseSensitive() {
        return settings.isDefaultCaseSensitive();
    }

    /**
     * Sets a value indicating if this instance of Sql2o is case sensitive when mapping between columns names and property
     * names. This should almost always be false, because most relational databases are not case sensitive.
     * @deprecated use {@link Sql2o#setSettings(Settings)} instead
     * @param defaultCaseSensitive
     */
    @Deprecated
    public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
        this.settings = this.settings.withDefaultCaseSensitive(defaultCaseSensitive);
    }

    /**
     * Gets value of {@link Settings} which is used in {@link Connection} instances created by this {@link Sql2o} object
     * @return
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Sets value of {@link Settings} which will be used in {@link Connection} instances
     * created by this {@link Sql2o} object.
     * <b>Note:</b> as long as {@link Settings} are immutable to update separate settings
     * {@code with*} methods should be used on instance got from {@link Sql2o#getSettings()} method.
     * For example: {@code sql2o.setSettings(sql2o.getSettings().withDefaultColumnMappings(newValue))}
     *
     * @param settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
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
    public Query createQuery(String query, boolean returnGeneratedKeys) {
        return new ReconnectableConnection(this.settings, this.getConnectionSource(), true).createQuery(query, returnGeneratedKeys);
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
    public Query createQuery(String query) {

        Connection connection = new ReconnectableConnection(this.settings, this.getConnectionSource(), true);
        return connection.createQuery(query);
    }

    /**
     * Opens a connection to the database
     * @param connectionSource the {@link ConnectionSource} implementation substitution,
     *                         that will be used instead of one from {@link Sql2o} instance.
     * @return instance of the {@link org.sql2o.Connection} class.
     */
    public Connection open(ConnectionSource connectionSource) {
        return new ReconnectableConnection(this.settings, connectionSource, false);
    }

    /**
     * Opens a connection to the database
     * @return instance of the {@link org.sql2o.Connection} class.
     */
    public Connection open() {
        checkConnectionSource("open");
        return new ReconnectableConnection(this.settings, this.getConnectionSource(), false);
    }

    /**
     * Wraps existing {@link java.sql.Connection} with {@link Connection Sql2o Connection}
     * @param jdbcConnection a connection to wrap
     * @return new unmanaged {@link org.sql2o.Connection} instance
     */
    public Connection wrap(java.sql.Connection jdbcConnection) {
        return new BaseConnection(jdbcConnection, settings, false);
    }

    /**
     * Invokes the run method on the {@link org.sql2o.StatementRunnableWithResult} instance. This method guarantees that
     * the connection is closed properly, when either the run method completes or if an exception occurs.
     * @param runnable
     * @param argument
     * @param <V>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <V> V withConnection(StatementRunnableWithResult<V> runnable, Object argument) {
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
        checkConnectionSource("beginTransaction");
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

        Connection connection = new ReconnectableConnection(this.settings, connectionSource, false);

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

    private void checkConnectionSource(String method) {
        if (connectionSource == null)
            throw new UnsupportedOperationException("you can't use Sql2o." + method +
                    " when no connectionSource is configured for Sql2o instance");
    }

    /**
     * Begins a transaction with isolation level {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}. Every statement executed on the return {@link Connection}
     * instance, will be executed in the transaction. It is very important to always call either the {@link org.sql2o.Connection#commit()}
     * method or the {@link org.sql2o.Connection#rollback()} method to close the transaction. Use proper try-catch logic.
     * @param connectionSource the {@link ConnectionSource} implementation substitution,
     *                         that will be used instead of one from {@link Sql2o} instance.
     *                         <b>Note:</b> if connectionSource is null then connectionSource
     *                         from {@link Sql2o} instance will be used that by default
     *                         means execution in dedicated transaction. Make sure in calling code that
     *                         your are not passing the {@code null} value if you dont expect this behaviour.
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

    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult){
        return runInTransaction(runnableWithResult, null);
    }
    
    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument){
        return runInTransaction(runnableWithResult, argument, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    @SuppressWarnings("unchecked")
    public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument, int isolationLevel){
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
