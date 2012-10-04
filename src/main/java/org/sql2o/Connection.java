package org.sql2o;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 10/12/11
 * Time: 10:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Connection {
    
    private final Logger logger = LoggerFactory.getLogger(Connection.class);

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

        try {
            if (this.getJdbcConnection().isClosed()){
                createConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Query q = new Query(this, queryText, name);
        return q;
    }
    
    public Query createQuery(String queryText){
        return createQuery(queryText, null);
    }


    /**
     * @deprecated - use on of the Sql2o.runInTransaction overloads instead. If an Exception is thrown within transaction scope,
     * the transaction will automatically be rolled back. It should never be necessary to call this method.
     */
    @Deprecated public Sql2o rollback(){
        try {
            this.getJdbcConnection().rollback();
        }
        catch (SQLException e) {
            logger.warn("Could not role back transaction. message: {}", e.getMessage());
        }
        finally {
            try {
                if (!this.getJdbcConnection().isClosed()){
                    this.getJdbcConnection().close();
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return this.getSql2o();
    }

    /**
     * @deprecated - use on of the Sql2o.runInTransaction overloads instead. If no exception is thrown within transaction scope,
     * the transaction will automatically be committed.
     */
    @Deprecated public Sql2o commit(){
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
}
