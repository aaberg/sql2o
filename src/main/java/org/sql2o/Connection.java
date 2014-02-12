package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Represents a connection to the database with a transaction.
 */
public class Connection implements AutoCloseable {
    
    private final Logger logger = LocalLoggerFactory.getLogger(Connection.class);

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

    public Connection(Sql2o sql2o) {

        this.sql2o = sql2o;
        createConnection();
    }

    void onException() {
        if (isRollbackOnException()) {
            rollback();
        }
    }

    public java.sql.Connection getJdbcConnection() {
        return jdbcConnection;
    }

    public Sql2o getSql2o() {
        return sql2o;
    }

    public Query createQuery(String queryText, String name){
        // If postgresql, the default behaviour should be not to retur generated keys, as this will throw an exception on
        // every query that does not create any new keys.
        boolean returnGeneratedKeys = !(this.sql2o.quirksMode == QuirksMode.PostgreSQL);
        return createQuery(queryText, name, returnGeneratedKeys);
    }

    public Query createQuery(String queryText, String name, boolean returnGeneratedKeys){

        try {
            if (this.getJdbcConnection().isClosed()){
                createConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Query q = new Query(this, queryText, name, returnGeneratedKeys);
        return q;
    }
    
    public Query createQuery(String queryText){
        return createQuery(queryText, null);
    }

    public Query createQuery(String queryText, boolean returnGeneratedKeys) {
        return createQuery(queryText, null, returnGeneratedKeys);
    }

    public Sql2o rollback(){
        try {
            this.getJdbcConnection().rollback();
        }
        catch (SQLException e) {
            logger.warn("Could not roll back transaction. message: {}", e);
        }
        finally {
            try {
                if (!this.getJdbcConnection().isClosed()){
                    this.getJdbcConnection().close();
                }
            }
            catch (SQLException e) {
                logger.warn("Could not close connection. message: {}", e);
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
        try{
            this.jdbcConnection = this.getSql2o().getDataSource().getConnection();
        }
        catch(Exception ex){
            throw new RuntimeException(String.format("Could not aquire a connection from DataSource - ", ex.getMessage()), ex);
        }
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
            this.keys.add(rs.getObject(1));
        }
    }

    public Object getKey(){
        if (!isCanGetKeys()){
            throw new Sql2oException("Keys where not fetched from database. Please call executeUpdate(true) to fetch keys");
        }
        if (this.keys != null && this.keys.size() > 0){
            return  keys.get(0);
        }
        return null;
    }
    
    public <V> V getKey(Class returnType){
        Object key = getKey();
        try {
            Converter converter = Convert.getConverter(returnType);
            return (V)converter.convert(key);
        } catch (ConverterException e) {
            throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
        }
    }

    public Object[] getKeys(){
        if (!isCanGetKeys()){
            throw new Sql2oException("Keys where not fetched from database. Please call executeUpdate() to fetch keys");
        }
        if (this.keys != null){
            return this.keys.toArray();
        }
        return null;
    }

    public boolean isCanGetKeys() {
        return canGetKeys;
    }

    void setCanGetKeys(boolean canGetKeys) {
        this.canGetKeys = canGetKeys;
    }

    public void close() throws Exception {
        if (!this.getJdbcConnection().isClosed()){
            this.rollback();
        }
    }
}
