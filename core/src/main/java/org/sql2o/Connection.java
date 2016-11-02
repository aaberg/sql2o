package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;
import org.sql2o.quirks.Quirks;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sql2o.converters.Convert.throwIfNull;

/**
 * Represents a connection to the database with a transaction.
 */
public class Connection implements AutoCloseable, Closeable {
    
    private final static Logger logger = LocalLoggerFactory.getLogger(Connection.class);

    private java.sql.Connection jdbcConnection;
    private Sql2o sql2o;

    private Integer result = null;
    private int[] batchResult = null;
    private List<Object> keys;
    private boolean canGetKeys;
    
    private boolean rollbackOnException = true;

    public boolean isRollbackOnException() {
        return rollbackOnException;
    }

    public Connection setRollbackOnException(boolean rollbackOnException) {
        this.rollbackOnException = rollbackOnException;
        return this;
    }

    private boolean rollbackOnClose = true;

    public boolean isRollbackOnClose() {
        return rollbackOnClose;
    }

    public Connection setRollbackOnClose(boolean rollbackOnClose) {
        this.rollbackOnClose = rollbackOnClose;
        return this;
    }

    final boolean autoClose;

    Connection(Sql2o sql2o, boolean autoClose) {

        this.autoClose = autoClose;
        this.sql2o = sql2o;
        createConnection();
    }

    void onException() {
        if (isRollbackOnException()) {
            rollback(this.autoClose);
        }
    }

    public java.sql.Connection getJdbcConnection() {
        return jdbcConnection;
    }

    public Sql2o getSql2o() {
        return sql2o;
    }

    public Query createQuery(String queryText){
        boolean returnGeneratedKeys = this.sql2o.getQuirks().returnGeneratedKeysByDefault();
        return createQuery(queryText, returnGeneratedKeys);
    }

    public Query createQuery(String queryText, boolean returnGeneratedKeys){

        try {
            if (jdbcConnection.isClosed()){
                createConnection();
            }
        } catch (SQLException e) {
            throw new Sql2oException("Error creating connection", e);
        }

        return new Query(this, queryText, returnGeneratedKeys);
    }

    public Query createQuery(String queryText, String ... columnNames) {
        try {
            if (jdbcConnection.isClosed()) {
                createConnection();
            }
        } catch(SQLException e) {
            throw new Sql2oException("Error creating connection", e);
        }

        return new Query(this, queryText, columnNames);
    }

    public Query createQueryWithParams(String queryText, Object... paramValues){
        // due to #146, creating a query will not create a statement anymore;
        // the PreparedStatement will only be created once the query needs to be executed
        // => there is no need to handle the query closing here anymore since there is nothing to close
        return createQuery(queryText)
                .withParams(paramValues);
    }

    public Sql2o rollback(){
        return this.rollback(true).sql2o;
    }

    public Connection rollback(boolean closeConnection){
        try {
            jdbcConnection.rollback();
        }
        catch (SQLException e) {
            logger.warn("Could not roll back transaction. message: {}", e);
        }
        finally {
            if(closeConnection) this.closeJdbcConnection();
        }
        return this;
    }

    public Sql2o commit(){
        return this.commit(true).sql2o;
    }

    public Connection commit(boolean closeConnection){
        try {
            jdbcConnection.commit();
        }
        catch (SQLException e) {
            throw new Sql2oException(e);
        }
        finally {
            if(closeConnection)
                this.closeJdbcConnection();
        }
        return this;
    }

    public int getResult(){
        if (this.result == null){
            throw new Sql2oException("It is required to call executeUpdate() method before calling getResult().");
        }
        return this.result;
    }

    void setResult(int result){
        this.result = result;
    }

    public int[] getBatchResult() {
        if (this.batchResult == null){
            throw new Sql2oException("It is required to call executeBatch() method before calling getBatchResult().");
        }
        return this.batchResult;
    }

    void setBatchResult(int[] value) {
        this.batchResult = value;
    }

    void setKeys(ResultSet rs) throws SQLException {
        if (rs == null){
            this.keys = null;
            return;
        }
        this.keys = new ArrayList<Object>();
        while(rs.next()){
            for (int column=1; column <= rs.getMetaData().getColumnCount(); column++) {
                this.keys.add(rs.getObject(column));
            }
        }
    }

    public Object getKey(){
        if (!this.canGetKeys){
            throw new Sql2oException("Keys were not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
        }
        if (this.keys != null && this.keys.size() > 0){
            return  keys.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked") // need to change Convert
    public <V> V getKey(Class returnType){
        final Quirks quirks = this.sql2o.getQuirks();
        Object key = getKey();
        try {
            Converter<V> converter = throwIfNull(returnType, quirks.converterOf(returnType));
            return converter.convert(key);
        } catch (ConverterException e) {
            throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
        }
    }

    public Object[] getKeys(){
        if (!this.canGetKeys){
            throw new Sql2oException("Keys where not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
        }
        if (this.keys != null){
            return this.keys.toArray();
        }
        return null;
    }

    @SuppressWarnings("unchecked") // need to change Convert
    public <V> List<V> getKeys(Class<V> returnType) {
        final Quirks quirks = sql2o.getQuirks();
        if (!this.canGetKeys) {
            throw new Sql2oException("Keys where not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
        }

        if (this.keys != null) {
            try {
                Converter<V> converter = throwIfNull(returnType, quirks.converterOf(returnType));

                List<V> convertedKeys = new ArrayList<V>(this.keys.size());

                for (Object key : this.keys) {
                    convertedKeys.add(converter.convert(key));
                }

                return convertedKeys;
            }
            catch (ConverterException e) {
                throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
            }
        }

        return null;
    }

    void setCanGetKeys(boolean canGetKeys) {
        this.canGetKeys = canGetKeys;
    }

    private final Set<Statement> statements = new HashSet<>();

    void registerStatement(Statement statement){
        statements.add(statement);
    }
    void removeStatement(Statement statement){
        statements.remove(statement);
    }

    public void close() {
        boolean connectionIsClosed;
        try {
            connectionIsClosed = jdbcConnection.isClosed();
        } catch (SQLException e) {
            throw new Sql2oException("Sql2o encountered a problem while trying to determine whether the connection is closed.", e);
        }

        if (!connectionIsClosed) {

            for (Statement statement : statements) {
                try {
                    getSql2o().getQuirks().closeStatement(statement);
                } catch (Throwable e) {
                    logger.warn("Could not close statement.", e);
                }
            }
            statements.clear();

            boolean rollback = rollbackOnClose;
            if (rollback) {
                try {
                    rollback = !jdbcConnection.getAutoCommit();
                } catch (SQLException e) {
                    logger.warn("Could not determine connection auto commit mode.", e);
                }
            }

            // if in transaction, rollback, otherwise just close
            if (rollback) {
                this.rollback(true);
            }
            else {
                this.closeJdbcConnection();
            }
        }
    }

    private void createConnection(){
        try{
            this.jdbcConnection = this.sql2o.getDataSource().getConnection();
        }
        catch(Exception ex){
            throw new Sql2oException("Could not acquire a connection from DataSource - " + ex.getMessage(), ex);
        }
    }

    private void closeJdbcConnection() {
        try {
            jdbcConnection.close();
        }
        catch (SQLException e) {
            logger.warn("Could not close connection. message: {}", e);
        }
    }
}
