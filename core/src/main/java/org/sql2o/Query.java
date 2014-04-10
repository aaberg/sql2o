package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.data.LazyTable;
import org.sql2o.data.Row;
import org.sql2o.data.Table;
import org.sql2o.data.TableResultSetIterator;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.PojoIntrospector;
import org.sql2o.tools.NamedParameterHandler;
import org.sql2o.tools.NamedParameterHandlerFactory;
import org.sql2o.tools.ResultSetUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

/**
 * Represents a sql2o statement. With sql2o, all statements are instances of the Query class.
 */
@SuppressWarnings("UnusedDeclaration")
public class Query {

    private final Logger logger = LocalLoggerFactory.getLogger(Query.class);

    private Connection connection;
    private Map<String, String> caseSensitiveColumnMappings;
    private Map<String, String> columnMappings;
    private final PreparedStatement statement;
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private String name;
    private boolean returnGeneratedKeys;

    private ResultSetHandlerFactoryBuilder resultSetHandlerFactoryBuilder;

    private final NamedParameterHandler namedParameterHandler;
    private final String parsedQuery;

    public Query(Connection connection, NamedParameterHandlerFactory namedParameterHandlerFactory, String queryText, String name, boolean returnGeneratedKeys) {
        this.connection = connection;
        this.name = name;
        this.returnGeneratedKeys = returnGeneratedKeys;
        this.setColumnMappings(connection.getSql2o().getDefaultColumnMappings());
        this.caseSensitive = connection.getSql2o().isDefaultCaseSensitive();
        this.namedParameterHandler = namedParameterHandlerFactory.newParameterHandler();

        parsedQuery = getNamedParameterHandler().parseStatement(queryText);
        try {
            if (returnGeneratedKeys) {
                statement = getConnection().getJdbcConnection().prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
            } else {
                statement = getConnection().getJdbcConnection().prepareStatement(parsedQuery);
            }
        } catch(SQLException ex) {
            throw new RuntimeException(String.format("Error preparing statement - %s", ex.getMessage()), ex);
        }
    }

    // ------------------------------------------------
    // ------------- Getter/Setters -------------------
    // ------------------------------------------------

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Query setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    public boolean isAutoDeriveColumnNames() {
        return autoDeriveColumnNames;
    }

    public Query setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
        this.autoDeriveColumnNames = autoDeriveColumnNames;
        return this;
    }

    public Connection getConnection(){
        return this.connection;
    }

    public String getName() {
        return name;
    }

    public NamedParameterHandler getNamedParameterHandler() {
        return namedParameterHandler;
    }

    public ResultSetHandlerFactoryBuilder getResultSetHandlerFactoryBuilder() {
        if (resultSetHandlerFactoryBuilder == null) {
            resultSetHandlerFactoryBuilder = new DefaultResultSetHandlerFactoryBuilder();
        }
        return resultSetHandlerFactoryBuilder;
    }

    public void setResultSetHandlerFactoryBuilder(ResultSetHandlerFactoryBuilder resultSetHandlerFactoryBuilder) {
        this.resultSetHandlerFactoryBuilder = resultSetHandlerFactoryBuilder;
    }

    // ------------------------------------------------
    // ------------- Add Parameters -------------------
    // ------------------------------------------------

    private void addParameterInternal(String name, ParameterSetter parameterSetter) {
        for (int paramIdx : this.getNamedParameterHandler().getParameterIndices(name)) {
            try {
                parameterSetter.setParameter(paramIdx);
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Error adding parameter '%s' - %s", name, e.getMessage()), e);
            }
        }
    }

    public Query addParameter(final String name, final Object value) {
        final Object convertedValue = convertParameter(value);

        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                statement.setObject(paramIdx, convertedValue);
            }
        });

        return this;
    }

    private Object convertParameter(Object value) {
        if (value == null) {
            return null;
        }
        Converter converter = Convert.getConverterIfExists(value.getClass());
        if (converter == null) {
            return null;
        }
        //noinspection unchecked
        return converter.toDatabaseParam(value);
    }

    public Query addParameter(final String name, final InputStream value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                statement.setBinaryStream(paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final int value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                statement.setInt(paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Integer value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                if (value == null) {
                    statement.setNull(paramIdx, Types.INTEGER);
                } else {
                    statement.setInt(paramIdx, value);
                }
            }
        });

        return this;
    }

    public Query addParameter(String name, final long value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                statement.setLong(paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Long value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                if (value == null) {
                    statement.setNull(paramIdx, Types.BIGINT);
                } else {
                    statement.setLong(paramIdx, value);
                }
            }
        });

        return this;
    }

    public Query addParameter(String name, final String value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                if (value == null) {
                    statement.setNull(paramIdx, Types.VARCHAR);
                } else {
                    statement.setString(paramIdx, value);
                }
            }
        });

        return this;
    }

    public Query addParameter(final String name, final Timestamp value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                if (value == null) {
                    statement.setNull(paramIdx, Types.TIMESTAMP);
                } else {
                    statement.setTimestamp(paramIdx, value);
                }
            }
        });

        return this;
    }

    public Query addParameter(final String name, final Time value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx) throws SQLException {
                if (value == null) {
                    statement.setNull(paramIdx, Types.TIME);
                } else {
                    statement.setTime(paramIdx, value);
                }
            }
        });

        return this;
    }

    public Query bind(final Object pojo) {
        Class clazz = pojo.getClass();
        Map<String, PojoIntrospector.ReadableProperty> propertyMap = PojoIntrospector.readableProperties(clazz);
        for (PojoIntrospector.ReadableProperty property : propertyMap.values()) {
            try {
                if( getNamedParameterHandler().containsParameter(property.name))
                    this.addParameter(property.name, property.get(pojo));
            }
            catch(IllegalArgumentException ex) {
                logger.debug("Ignoring Illegal Arguments", ex);
            }
            catch(IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this;
    }

    // ------------------------------------------------
    // -------------------- Execute -------------------
    // ------------------------------------------------

    /**
     * Iterable {@link java.sql.ResultSet} that wraps {@link PojoResultSetIterator}.
     */
    private abstract class ResultSetIterableBase<T> implements ResultSetIterable<T> {
        private long start;
        private long afterExecQuery;
        protected ResultSet rs;

        public ResultSetIterableBase() {
            try {
                start = System.currentTimeMillis();
                rs = statement.executeQuery();
                afterExecQuery = System.currentTimeMillis();
            }
            catch (SQLException ex) {
                throw new Sql2oException("Database error: " + ex.getMessage(), ex);
            }
        }

        public void close() {
            try {
                if (rs != null) {
                    rs.close();

                    // log the query
                    long afterClose = System.currentTimeMillis();
                    logger.debug("total: {} ms, execution: {} ms, reading and parsing: {} ms; executed [{}]", new Object[]{
                            afterClose - start,
                            afterExecQuery-start,
                            afterClose - afterExecQuery,
                            name
                    });

                    rs = null;
                }
            }
            catch (SQLException ex) {
                throw new Sql2oException("Error closing ResultSet.", ex);
            }
            finally {
                closeConnectionIfNecessary();
            }
        }
    }

    /**
     * Read a collection lazily. Generally speaking, this should only be used if you are reading MANY
     * results and keeping them all in a Collection would cause memory issues. You MUST call
     * {@link org.sql2o.ResultSetIterable#close()} when you are done iterating.
     *
     * @param returnType type of each row
     * @return iterable results
     */
    public <T> ResultSetIterable<T> executeAndFetchLazy(final Class<T> returnType) {
        final Quirks quirks = getConnection().getSql2o().getQuirks();
        ResultSetHandlerFactoryBuilder builder = getResultSetHandlerFactoryBuilder();
        if(builder==null) builder=new DefaultResultSetHandlerFactoryBuilder();
        builder.setAutoDeriveColumnNames(autoDeriveColumnNames);
        builder.setCaseSensitive(caseSensitive);
        builder.setColumnMappings(columnMappings);
        builder.setQuirks(quirks);
        final ResultSetHandlerFactory<T> resultSetHandlerFactory = builder.newFactory(returnType);
        return new ResultSetIterableBase<T>() {
            public Iterator<T> iterator() {
                return new PojoResultSetIterator<T>(rs, isCaseSensitive(), quirks, resultSetHandlerFactory);
            }
        };
    }

    public <T> List<T> executeAndFetch(Class<T> returnType){
        List<T> list = new ArrayList<T>();

        // if sql2o moves to java 7 at some point, this could be much cleaner using try-with-resources
        ResultSetIterable<T> iterable = null;
        try {
            iterable = executeAndFetchLazy(returnType);
            for (T item : iterable) {
                list.add(item);
            }
        }
        finally {
            if (iterable != null) {
                iterable.close();
            }
        }

        return list;
    }

    public <T> T executeAndFetchFirst(Class<T> returnType){
        // if sql2o moves to java 7 at some point, this could be much cleaner using try-with-resources
        ResultSetIterable<T> iterable = null;
        try {
            iterable = executeAndFetchLazy(returnType);
            Iterator<T> iterator = iterable.iterator();
            return iterator.hasNext() ? iterator.next() : null;
        }
        finally {
            if (iterable != null) {
                iterable.close();
            }
        }
    }

    public LazyTable executeAndFetchTableLazy() {
        final LazyTable lt = new LazyTable();

        lt.setRows(new ResultSetIterableBase<Row>() {
            public Iterator<Row> iterator() {
                return new TableResultSetIterator(rs, isCaseSensitive(), getConnection().getSql2o().getQuirks(), lt);
            }
        });

        return lt;
    }

    public Table executeAndFetchTable() {
        LazyTable lt = null;

        List<Row> rows = new ArrayList<Row>();

        try {
            lt = executeAndFetchTableLazy();

            for (Row item : lt.rows()) {
                rows.add(item);
            }
        }
        finally {
            if (lt != null) {
                lt.close();
            }
        }

        return lt == null ? null : new Table(lt.getName(), rows, lt.columns());
    }

    public Connection executeUpdate(){
        long start = System.currentTimeMillis();
        try{
            this.connection.setResult(statement.executeUpdate());
            this.connection.setKeys(this.returnGeneratedKeys ? statement.getGeneratedKeys() : null);
            connection.setCanGetKeys(this.returnGeneratedKeys);
        }
        catch(SQLException ex){
            this.connection.onException();
            throw new Sql2oException("Error in executeUpdate, " + ex.getMessage(), ex);
        }
        finally {
            closeConnectionIfNecessary();
        }

        long end = System.currentTimeMillis();
        logger.debug("total: {} ms; executed update [{}]", new Object[]{
                end - start,
                this.getName() == null ? "No name" : this.getName()
        });

        return this.connection;
    }

    public Object executeScalar(){
        long start = System.currentTimeMillis();
        try {
            ResultSet rs = this.statement.executeQuery();
            if (rs.next()){
                Object o = ResultSetUtils.getRSVal(rs, 1);
                long end = System.currentTimeMillis();
                logger.debug("total: {} ms; executed scalar [{}]", new Object[]{
                        end - start,
                        this.getName() == null ? "No name" : this.getName()
                });
                return o;
            }
            else{
                return null;
            }

        }
        catch (SQLException e) {
            this.connection.onException();
            throw new Sql2oException("Database error occurred while running executeScalar: " + e.getMessage(), e);
        }
        finally{
            closeConnectionIfNecessary();
        }

    }

    public <V> V executeScalar(Class<V> returnType){
        Object value = executeScalar();
        Converter<V> converter;
        try {
            //noinspection unchecked
            converter = Convert.getConverter(returnType);
            return converter.convert(value);
        } catch (ConverterException e) {
            throw new Sql2oException("Error occured while converting value from database to type " + returnType.toString(), e);
        }

    }

    public <T> List<T> executeScalarList(Class<T> returnType){
        long start = System.currentTimeMillis();
        List<T> list = new ArrayList<T>();
        try{
            //noinspection unchecked
            Converter<T> converter = Convert.getConverter(returnType);
            ResultSet rs = this.statement.executeQuery();
            while(rs.next()){
                Object value = ResultSetUtils.getRSVal(rs, 1);
                list.add(converter.convert(value));
            }

            long end = System.currentTimeMillis();
            logger.debug("total: {} ms; executed scalar list [{}]", new Object[]{
                    end - start,
                    this.getName() == null ? "No name" : this.getName()
            });

            return list;
        }
        catch(SQLException ex){
            this.connection.onException();
            throw new Sql2oException("Error occurred while executing scalar list: " + ex.getMessage(), ex);
        }
        catch (ConverterException e) {
            throw new Sql2oException("Error occurred while converting value from database to type " + returnType.toString(), e);
        }
        finally{
            closeConnectionIfNecessary();
        }
    }

    /************** batch stuff *******************/

    public Query addToBatch(){
        try {
            statement.addBatch();
        } catch (SQLException e) {
            throw new Sql2oException("Error while adding statement to batch", e);
        }

        return this;
    }

    public Connection executeBatch() throws Sql2oException {
        long start = System.currentTimeMillis();
        try {
            connection.setBatchResult(statement.executeBatch());
            connection.setKeys(this.returnGeneratedKeys ? statement.getGeneratedKeys() : null);
            connection.setCanGetKeys(this.returnGeneratedKeys);
        }
        catch (Throwable e) {
            this.connection.onException();
            throw new Sql2oException("Error while executing batch operation: " + e.getMessage(), e);
        }
        finally {
            closeConnectionIfNecessary();
        }

        long end = System.currentTimeMillis();
        logger.debug("total: {} ms; executed batch [{}]", new Object[]{
                end - start,
                this.getName() == null ? "No name" : this.getName()
        });

        return this.connection;
    }

    /*********** column mapping ****************/

    public Map<String, String> getColumnMappings() {
        if (this.isCaseSensitive()){
            return this.caseSensitiveColumnMappings;
        }
        else{
            return this.columnMappings;
        }
    }

    public Query setColumnMappings(Map<String, String> mappings){

        this.caseSensitiveColumnMappings = new HashMap<String, String>();
        this.columnMappings = new HashMap<String, String>();

        for (Map.Entry<String,String> entry : mappings.entrySet()){
            this.caseSensitiveColumnMappings.put(entry.getKey(), entry.getValue());
            this.columnMappings.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
        }

        return this;
    }

    public Query addColumnMapping(String columnName, String propertyName){
        this.caseSensitiveColumnMappings.put(columnName, propertyName);
        this.columnMappings.put(columnName.toLowerCase(), propertyName.toLowerCase());

        return this;
    }

    /************** private stuff ***************/

    private void closeConnectionIfNecessary(){
        try{
            if (connection.autoClose && !connection.getJdbcConnection().isClosed() && statement != null){
                this.statement.close();
                this.connection.getJdbcConnection().close();
            }
        }
        catch (Exception ex){
            throw new RuntimeException("Error while attempting to close connection", ex);
        }
    }

    private interface ParameterSetter{
        void setParameter(int paramIdx) throws SQLException;
    }
}
