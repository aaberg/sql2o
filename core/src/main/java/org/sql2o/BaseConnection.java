package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;
import org.sql2o.quirks.Quirks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.sql2o.converters.Convert.throwIfNull;

/**
 * Created by nickl on 20.01.17.
 */
class BaseConnection implements Connection {
    private final static Logger logger = LocalLoggerFactory.getLogger(BaseConnection.class);
    protected final Settings settings;
    private final boolean autoClose;
    private final Set<Statement> statements = new HashSet<>();
    protected java.sql.Connection jdbcConnection;
    private Integer result = null;
    private int[] batchResult = null;
    private List<Object> keys;
    private boolean canGetKeys;
    private boolean rollbackOnException = true;
    private boolean rollbackOnClose = true;

    BaseConnection(Settings settings, boolean autoClose) {
        this.settings = requireNonNull(settings, "settings can't be null");
        this.autoClose = autoClose;
    }

    BaseConnection(java.sql.Connection jdbcConnection, Settings settings, boolean autoClose) {
       this(settings, autoClose);
       this.jdbcConnection = Objects.requireNonNull(jdbcConnection, "jdbcConnection can't be null");
    }

    @Override
    public boolean isRollbackOnException() {
        return rollbackOnException;
    }

    @Override
    public Connection setRollbackOnException(boolean rollbackOnException) {
        this.rollbackOnException = rollbackOnException;
        return this;
    }

    @Override
    public boolean isRollbackOnClose() {
        return rollbackOnClose;
    }

    @Override
    public Connection setRollbackOnClose(boolean rollbackOnClose) {
        this.rollbackOnClose = rollbackOnClose;
        return this;
    }


    public void onException() {
        if (isRollbackOnException()) {
            rollback(this.autoClose);
        }
    }

    @Override
    public java.sql.Connection getJdbcConnection() {
        return jdbcConnection;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public Query createQuery(String queryText, boolean returnGeneratedKeys){
        return new Query(this, queryText, returnGeneratedKeys);
    }

    @Override
    public Query createQuery(String queryText, String... columnNames) {
        return new Query(this, queryText, columnNames);
    }

    @Override
    public Query createQuery(String queryText){
        boolean returnGeneratedKeys = this.settings.getQuirks().returnGeneratedKeysByDefault();
        return createQuery(queryText, returnGeneratedKeys);
    }

    @Override
    public Query createQueryWithParams(String queryText, Object... paramValues){
        // due to #146, creating a query will not create a statement anymore;
        // the PreparedStatement will only be created once the query needs to be executed
        // => there is no need to handle the query closing here anymore since there is nothing to close
        return createQuery(queryText)
                .withParams(paramValues);
    }

    @Override
    public void rollback(){
       this.rollback(true);
    }

    @Override
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

    @Override
    public void commit(){
        this.commit(true);
    }

    @Override
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

    @Override
    public int getResult(){
        if (this.result == null){
            throw new Sql2oException("It is required to call executeUpdate() method before calling getResult().");
        }
        return this.result;
    }


    public void setResult(int result){
        this.result = result;
    }

    @Override
    public int[] getBatchResult() {
        if (this.batchResult == null){
            throw new Sql2oException("It is required to call executeBatch() method before calling getBatchResult().");
        }
        return this.batchResult;
    }


    public void setBatchResult(int[] value) {
        this.batchResult = value;
    }


    public void setKeys(ResultSet rs) throws SQLException {
        if (rs == null){
            this.keys = null;
            return;
        }
        this.keys = new ArrayList<Object>();
        while(rs.next()){
            this.keys.add(rs.getObject(1));
        }
    }

    @Override
    public Object getKey(){
        if (!this.canGetKeys){
            throw new Sql2oException("Keys were not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
        }
        if (this.keys != null && this.keys.size() > 0){
            return  keys.get(0);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked") // need to change Convert
    public <V> V getKey(Class returnType){
        final Quirks quirks = this.settings.getQuirks();
        Object key = getKey();
        try {
            Converter<V> converter = throwIfNull(returnType, quirks.converterOf(returnType));
            return converter.convert(key);
        } catch (ConverterException e) {
            throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
        }
    }

    @Override
    public Object[] getKeys(){
        if (!this.canGetKeys){
            throw new Sql2oException("Keys where not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
        }
        if (this.keys != null){
            return this.keys.toArray();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked") // need to change Convert
    public <V> List<V> getKeys(Class<V> returnType) {
        final Quirks quirks = settings.getQuirks();
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


    public void setCanGetKeys(boolean canGetKeys) {
        this.canGetKeys = canGetKeys;
    }


    public void registerStatement(Statement statement){
        statements.add(statement);
    }


    public void removeStatement(Statement statement){
        statements.remove(statement);
    }

    @Override
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
                    settings.getQuirks().closeStatement(statement);
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


    public void closeIfNecessary() {
        try{
            if (this.autoClose){
                this.close();
            }
        }
        catch (Exception ex){
            throw new Sql2oException("Error while attempting to close connection", ex);
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
