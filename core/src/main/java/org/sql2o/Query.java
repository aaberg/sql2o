package org.sql2o;

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

import java.io.InputStream;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import static org.sql2o.converters.Convert.throwIfNull;

/**
 * Represents a sql2o statement. With sql2o, all statements are instances of the Query class.
 */
@SuppressWarnings("UnusedDeclaration")
public class Query implements AutoCloseable {

    private final static Logger logger = LocalLoggerFactory.getLogger(Query.class);

    private Connection connection;
    private Map<String, String> caseSensitiveColumnMappings;
    private Map<String, String> columnMappings;
    private PreparedStatement preparedStatement = null;
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private boolean throwOnMappingFailure = true;
    private String name;
    private boolean returnGeneratedKeys;
    private final String[] columnNames;
    private final Map<String, List<Integer>> paramNameToIdxMap;
    private final Map<String, ParameterSetter> parameters;
    private String parsedQuery;
    private int maxBatchRecords = 0;
    private int currentBatchRecords = 0;

    private ResultSetHandlerFactoryBuilder resultSetHandlerFactoryBuilder;

    @Override
    public String toString() {
        return parsedQuery;
    }

    public Query(Connection connection, String queryText, boolean returnGeneratedKeys) {
        this(connection, queryText, returnGeneratedKeys, null);
    }

    public Query(Connection connection, String queryText, String[] columnNames) {
        this(connection, queryText, false, columnNames);
    }

    private Query(Connection connection, String queryText, boolean returnGeneratedKeys, String[] columnNames) {
        this.connection = connection;
        this.returnGeneratedKeys = returnGeneratedKeys;
        this.columnNames = columnNames;
        this.setColumnMappings(connection.getSql2o().getDefaultColumnMappings());
        this.caseSensitive = connection.getSql2o().isDefaultCaseSensitive();

        paramNameToIdxMap = new HashMap<>();
        parameters = new HashMap<>();

        parsedQuery = connection.getSql2o().getQuirks().getSqlParameterParsingStrategy().parseSql(queryText, paramNameToIdxMap);
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

    public Query throwOnMappingFailure(boolean throwOnMappingFailure) {
        this.throwOnMappingFailure = throwOnMappingFailure;
        return this;
    }

    public boolean isThrowOnMappingFailure() {
        return throwOnMappingFailure;
    }

    public Connection getConnection(){
        return this.connection;
    }

    public String getName() {
        return name;
    }

    public Query setName(String name) {
        this.name = name;
        return this;
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

    public Map<String, List<Integer>> getParamNameToIdxMap() {
        return paramNameToIdxMap;
    }

    // ------------------------------------------------
    // ------------- Add Parameters -------------------
    // ------------------------------------------------

    private void addParameterInternal(String name, ParameterSetter parameterSetter) {
        if (!this.getParamNameToIdxMap().containsKey(name)) {
            throw new Sql2oException("Failed to add parameter with name '" + name + "'. No parameter with that name is declared in the sql.");
        }
        parameters.put(name, parameterSetter);
    }

    @SuppressWarnings("unchecked")
    private Object convertParameter(Object value) {
        if (value == null) {
            return null;
        }
        Converter converter = getQuirks().converterOf(value.getClass());
        if (converter == null) {
            // let's try to add parameter AS IS
            return value;
        }
        return converter.toDatabaseParam(value);
    }

    public <T> Query addParameter(String name, Class<T> parameterClass, T value){
        //TODO: must cover most of types: BigDecimal,Boolean,SmallInt,Double,Float,byte[]
        if(InputStream.class.isAssignableFrom(parameterClass))
            return addParameter(name, (InputStream)value);
        if(Integer.class==parameterClass)
            return addParameter(name, (Integer)value);
        if(Long.class==parameterClass)
            return addParameter(name, (Long)value);
        if(String.class==parameterClass)
            return addParameter(name, (String)value);
        if(Timestamp.class==parameterClass)
            return addParameter(name, (Timestamp)value);
        if(Time.class==parameterClass)
            return addParameter(name, (Time)value);

        if(parameterClass.isArray()
                // byte[] is used for blob already
                && byte[].class != parameterClass) {
            return addParameter(name, toObjectArray(value));
        }
        if(Collection.class.isAssignableFrom(parameterClass)) {
            return addParameter(name, (Collection) value);
        }

        final Object convertedValue = convertParameter(value);

        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, convertedValue);
            }
        });

        return this;
    }

    public Query withParams(Object... paramValues){
        int i=0;
        for (Object paramValue : paramValues) {
            addParameter("p" + (++i), paramValue);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Query addParameter(String name, Object value) {
        return value == null
                ? addParameter(name, Object.class, null)
                : addParameter(name,
                    (Class<Object>) value.getClass(),
                    value);
    }

    public Query addParameter(String name, final InputStream value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final int value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Integer value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final long value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Long value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final String value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Timestamp value){
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final Time value) {
        addParameterInternal(name, new ParameterSetter() {
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });

        return this;
    }

    public Query addParameter(String name, final boolean value) {
        addParameterInternal(name, new ParameterSetter() {
            @Override
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });
        return this;
    }

    public Query addParameter(String name, final Boolean value) {
        addParameterInternal(name, new ParameterSetter() {
            @Override
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });
        return this;
    }

    public Query addParameter(String name, final UUID value) {
        addParameterInternal(name, new ParameterSetter() {
            @Override
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, value);
            }
        });
        return this;
    }

    /**
     * Set an array parameter.<br>
     * For example:
     * <pre>
     *     createQuery("SELECT * FROM user WHERE id IN(:ids)")
     *      .addParameter("ids", 4, 5, 6)
     *      .executeAndFetch(...)
     * </pre>
     * will generate the query : <code>SELECT * FROM user WHERE id IN(4,5,6)</code><br>
     * <br>
     * It is not possible to use array parameters with a batch <code>PreparedStatement</code>:
     * since the text query passed to the <code>PreparedStatement</code> depends on the number of parameters in the array,
     * array parameters are incompatible with batch mode.<br>
     * <br>
     * If the values array is empty, <code>null</code> will be set to the array parameter:
     * <code>SELECT * FROM user WHERE id IN(NULL)</code>
     *
     * @throws NullPointerException if values parameter is null
     */
    public Query addParameter(String name, final Object ... values) {
        if(values == null) {
            throw new NullPointerException("Array parameter cannot be null");
        }

        addParameterInternal(name, new ParameterSetter(values.length) {
            @Override
            public void setParameter(int paramIdx, PreparedStatement statement) throws SQLException {
                if(values.length == 0) {
                    getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx, (Object) null);
                } else {
                    for (Object value : values) {
                        getConnection().getSql2o().getQuirks().setParameter(statement, paramIdx++, value);
                    }
                }
            }
        });
        return this;
    }

    /**
     * Set an array parameter.<br>
     * See {@link #addParameter(String, Object...)} for details
     */
    public Query addParameter(String name, final Collection<?> values) {
        if(values == null) {
            throw new NullPointerException("Array parameter cannot be null");
        }

        return addParameter(name, values.toArray());
    }

    public Query bind(final Object pojo) {
        Class clazz = pojo.getClass();
        Map<String, PojoIntrospector.ReadableProperty> propertyMap = PojoIntrospector.readableProperties(clazz);
        for (PojoIntrospector.ReadableProperty property : propertyMap.values()) {
            try {
                if( this.getParamNameToIdxMap().containsKey(property.name)) {

                    @SuppressWarnings("unchecked")
                    final Class<Object> type = (Class<Object>) property.type;
                    this.addParameter(property.name, type, property.get(pojo));
                }
            }
            catch(IllegalArgumentException ex) {
                logger.debug("Ignoring Illegal Arguments", ex);
            }
            catch(IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this;
    }

    public void close() {
        if(preparedStatement != null) {
            connection.removeStatement(preparedStatement);
            try {
                this.getQuirks().closeStatement(preparedStatement);
            } catch (Throwable ex) {
                logger.warn("Could not close statement.", ex);
            }
        }
    }

    // ------------------------------------------------
    // -------------------- Execute -------------------
    // ------------------------------------------------

    // visible for testing
    PreparedStatement buildPreparedStatement() {
        return buildPreparedStatement(true);
    }

    private PreparedStatement buildPreparedStatement(boolean allowArrayParameters) {
        // array parameter handling
        parsedQuery = ArrayParameters.updateQueryAndParametersIndexes(parsedQuery, paramNameToIdxMap, parameters, allowArrayParameters);

        // prepare statement creation
        if(preparedStatement == null) {
            try {
                if (columnNames != null && columnNames.length > 0){
                    preparedStatement = connection.getJdbcConnection().prepareStatement(parsedQuery, columnNames);
                } else if (returnGeneratedKeys) {
                    preparedStatement = connection.getJdbcConnection().prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
                } else {
                    preparedStatement = connection.getJdbcConnection().prepareStatement(parsedQuery);
                }
            } catch(SQLException ex) {
                throw new Sql2oException(String.format("Error preparing statement - %s", ex.getMessage()), ex);
            }
            connection.registerStatement(preparedStatement);
        }

        // parameters assignation to query
        for(Map.Entry<String, ParameterSetter> parameter : parameters.entrySet()) {
            for (int paramIdx : paramNameToIdxMap.get(parameter.getKey())) {
                try {
                    parameter.getValue().setParameter(paramIdx, preparedStatement);
                } catch (SQLException e) {
                    throw new RuntimeException(String.format("Error adding parameter '%s' - %s", parameter.getKey(), e.getMessage()), e);
                }
            }
        }
        // the parameters need to be cleared, so in case of batch, only new parameters will be added
        parameters.clear();

        return preparedStatement;
    }

    /**
     * Iterable {@link java.sql.ResultSet} that wraps {@link PojoResultSetIterator}.
     */
    private abstract class ResultSetIterableBase<T> implements ResultSetIterable<T> {
        private long start;
        private long afterExecQuery;
        protected ResultSet rs;

        boolean autoCloseConnection = false;

        public ResultSetIterableBase() {
            try {
                start = System.currentTimeMillis();
                logExecution();
                rs = buildPreparedStatement().executeQuery();
                afterExecQuery = System.currentTimeMillis();
            }
            catch (SQLException ex) {
                throw new Sql2oException("Database error: " + ex.getMessage(), ex);
            }
        }

        @Override
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
                if (this.isAutoCloseConnection()){
                    connection.close();
                } else {
                    closeConnectionIfNecessary();
                }
            }
        }

        @Override
        public boolean isAutoCloseConnection() {
            return this.autoCloseConnection;
        }

        @Override
        public void setAutoCloseConnection(boolean autoCloseConnection) {
            this.autoCloseConnection = autoCloseConnection;
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
        final ResultSetHandlerFactory<T> resultSetHandlerFactory = newResultSetHandlerFactory(returnType);
        return executeAndFetchLazy(resultSetHandlerFactory);
    }

    private <T> ResultSetHandlerFactory<T> newResultSetHandlerFactory(Class<T> returnType) {
        final Quirks quirks = getConnection().getSql2o().getQuirks();
        ResultSetHandlerFactoryBuilder builder = getResultSetHandlerFactoryBuilder();
        if(builder==null) builder=new DefaultResultSetHandlerFactoryBuilder();
        builder.setAutoDeriveColumnNames(this.autoDeriveColumnNames);
        builder.setCaseSensitive(this.caseSensitive);
        builder.setColumnMappings(this.getColumnMappings());
        builder.setQuirks(quirks);
        builder.throwOnMappingError(this.throwOnMappingFailure);
        return builder.newFactory(returnType);
    }

    /**
     * Read a collection lazily. Generally speaking, this should only be used if you are reading MANY
     * results and keeping them all in a Collection would cause memory issues. You MUST call
     * {@link org.sql2o.ResultSetIterable#close()} when you are done iterating.
     *
     * @param resultSetHandlerFactory factory to provide ResultSetHandler
     * @return iterable results
     */
    public <T> ResultSetIterable<T> executeAndFetchLazy(final ResultSetHandlerFactory<T> resultSetHandlerFactory) {
        final Quirks quirks = getConnection().getSql2o().getQuirks();
        return new ResultSetIterableBase<T>() {
            public Iterator<T> iterator() {
                return new PojoResultSetIterator<>(rs, isCaseSensitive(), quirks, resultSetHandlerFactory);
            }
        };
    }

    /**
     * Read a collection lazily. Generally speaking, this should only be used if you are reading MANY
     * results and keeping them all in a Collection would cause memory issues. You MUST call
     * {@link org.sql2o.ResultSetIterable#close()} when you are done iterating.
     *
     * @param resultSetHandler ResultSetHandler
     * @return iterable results
     */
    public <T> ResultSetIterable<T> executeAndFetchLazy(final ResultSetHandler<T> resultSetHandler) {
        final ResultSetHandlerFactory<T> factory = newResultSetHandlerFactory(resultSetHandler);
        return executeAndFetchLazy(factory);
    }

    private static  <T> ResultSetHandlerFactory<T> newResultSetHandlerFactory(final ResultSetHandler<T> resultSetHandler) {
        return new ResultSetHandlerFactory<T>() {
            public ResultSetHandler<T> newResultSetHandler(ResultSetMetaData resultSetMetaData) throws SQLException {
                return resultSetHandler;
            }
        };
    }

    public <T> List<T> executeAndFetch(Class<T> returnType){
        return executeAndFetch(newResultSetHandlerFactory(returnType));
    }

    public <T> List<T> executeAndFetch(ResultSetHandler<T> resultSetHandler){
        return executeAndFetch(newResultSetHandlerFactory(resultSetHandler));
    }

    public <T> List<T> executeAndFetch(ResultSetHandlerFactory<T> factory){
        List<T> list = new ArrayList<>();

        try (ResultSetIterable<T> iterable = executeAndFetchLazy(factory)) {
            for (T item : iterable) {
                list.add(item);
            }
        }

        return list;
    }

    public <T> T executeAndFetchFirst(Class<T> returnType){
        return executeAndFetchFirst(newResultSetHandlerFactory(returnType));
    }

    public <T> T executeAndFetchFirst(ResultSetHandler<T> resultSetHandler){
        return executeAndFetchFirst(newResultSetHandlerFactory(resultSetHandler));
    }

    public <T> T executeAndFetchFirst(ResultSetHandlerFactory<T> resultSetHandlerFactory){

        try (ResultSetIterable<T> iterable = executeAndFetchLazy(resultSetHandlerFactory))  {
            Iterator<T> iterator = iterable.iterator();
            return iterator.hasNext() ? iterator.next() : null;
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
        List<Row> rows = new ArrayList<>();

        try  (LazyTable lt =  executeAndFetchTableLazy()){
            for (Row item : lt.rows()) {
                rows.add(item);
            }
             // lt==null is always false
            return new Table(lt.getName(), rows, lt.columns());

        }

    }

    public Connection executeUpdate(){
        long start = System.currentTimeMillis();
        try{
            logExecution();
            PreparedStatement statement = buildPreparedStatement();
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
            logExecution();
            ResultSet rs = buildPreparedStatement().executeQuery();
            if (rs.next()){
                Object o = getQuirks().getRSVal(rs, 1);
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

    private Quirks getQuirks() {
        return this.connection.getSql2o().getQuirks();
    }

    public <V> V executeScalar(Class<V> returnType){
        try {
            Converter<V> converter;
            //noinspection unchecked
            converter = throwIfNull(returnType, getQuirks().converterOf(returnType));
            //noinspection unchecked
            logExecution();
            return executeScalar(converter);
        } catch (ConverterException e) {
            throw new Sql2oException("Error occured while converting value from database to type " + returnType, e);
        }
    }

    public <V> V executeScalar(Converter<V> converter){
        try {
            //noinspection unchecked
            return converter.convert(executeScalar());
        } catch (ConverterException e) {
            throw new Sql2oException("Error occured while converting value from database", e);
        }
    }



    public <T> List<T> executeScalarList(final Class<T> returnType){
        return executeAndFetch(newScalarResultSetHandler(returnType));
    }

    @SuppressWarnings("unchecked")
    private <T> ResultSetHandler<T> newScalarResultSetHandler(final Class<T> returnType) {
        final Quirks quirks = getQuirks();
        try {
            final Converter<T> converter = throwIfNull(returnType, quirks.converterOf(returnType));
            return new ResultSetHandler<T>() {
                public T handle(ResultSet resultSet) throws SQLException {
                    Object value = quirks.getRSVal(resultSet, 1);
                    try {
                        return (converter.convert(value));
                    } catch (ConverterException e) {
                        throw new Sql2oException("Error occurred while converting value from database to type " + returnType, e);
                    }
                }
            };
        } catch (ConverterException e) {
            throw new Sql2oException("Can't get converter for type " + returnType, e);
        }
    }

    /************** batch stuff *******************/

    /**
     * Sets the number of batched commands this Query allows to be added
     * before implicitly calling <code>executeBatch()</code> from <code>addToBatch()</code>. <br/>
     *
     * When set to 0, executeBatch is not called implicitly. This is the default behaviour. <br/>
     *
     * When using this, please take care about calling <code>executeBatch()</code> after finished
     * adding all commands to the batch because commands may remain unexecuted after the
     * last <code>addToBatch()</code> call. Additionally, if fetchGeneratedKeys is set, then
     * previously generated keys will be lost after a batch is executed.
     *
     * @throws IllegalArgumentException Thrown if the value is negative.
     */
    public Query setMaxBatchRecords(int maxBatchRecords){
        if (maxBatchRecords < 0){
            throw new IllegalArgumentException("maxBatchRecords should be a nonnegative value");
        }
        this.maxBatchRecords = maxBatchRecords;
        return this;
    }

    public int getMaxBatchRecords(){
        return this.maxBatchRecords;
    }

    /**
     * @return The current number of unexecuted batched statements
     */
    public int getCurrentBatchRecords() {
        return this.currentBatchRecords;
    }

    /**
     * @return True if maxBatchRecords is set and there are unexecuted batched commands or
     * maxBatchRecords is not set
     */
    public boolean isExplicitExecuteBatchRequired(){
        return (this.maxBatchRecords > 0 && this.currentBatchRecords > 0) || (this.maxBatchRecords == 0);
    }

    /**
     * Adds a set of parameters to this <code>Query</code>
     * object's batch of commands. <br/>
     *
     * If maxBatchRecords is more than 0, executeBatch is called upon adding that many
     * commands to the batch. <br/>
     *
     * The current number of batched commands is accessible via the <code>getCurrentBatchRecords()</code>
     * method.
     */
    public Query addToBatch(){
        try {
            buildPreparedStatement(false).addBatch();
            if (this.maxBatchRecords > 0){
                if(++this.currentBatchRecords % this.maxBatchRecords == 0) {
                    this.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new Sql2oException("Error while adding statement to batch", e);
        }

        return this;
    }

    /**
     * Adds a set of parameters to this <code>Query</code>
     * object's batch of commands and returns any generated keys. <br/>
     *
     * If maxBatchRecords is more than 0, executeBatch is called upon adding that many
     * commands to the batch. This method will return any generated keys if <code>fetchGeneratedKeys</code> is set. <br/>
     *
     * The current number of batched commands is accessible via the <code>getCurrentBatchRecords()</code>
     * method.
     */
    public <A> List<A> addToBatchGetKeys(Class<A> klass){
        this.addToBatch();

        if (this.currentBatchRecords == 0) {
            return this.connection.getKeys(klass);
        } else {
            return Collections.emptyList();
        }
    }

    public Connection executeBatch() throws Sql2oException {
        long start = System.currentTimeMillis();
        try {
            logExecution();
            PreparedStatement statement = buildPreparedStatement();
            connection.setBatchResult(statement.executeBatch());
            this.currentBatchRecords = 0;
            try {
                connection.setKeys(this.returnGeneratedKeys ? statement.getGeneratedKeys() : null);
                connection.setCanGetKeys(this.returnGeneratedKeys);
            } catch (SQLException sqlex) {
                throw new Sql2oException("Error while trying to fetch generated keys from database. If you are not expecting any generated keys, fix this error by setting the fetchGeneratedKeys parameter in the createQuery() method to 'false'", sqlex);
            }
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

        this.caseSensitiveColumnMappings = new HashMap<>();
        this.columnMappings = new HashMap<>();

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
            if (connection.autoClose){
                connection.close();
            }
        }
        catch (Exception ex){
            throw new Sql2oException("Error while attempting to close connection", ex);
        }
    }

    private void logExecution() {
        logger.debug("Executing query:{}{}", new Object[]{ System.lineSeparator(), this.parsedQuery } );
    }

    // from http://stackoverflow.com/questions/5606338/cast-primitive-type-array-into-object-array-in-java
    private static Object[] toObjectArray(Object val){
        if (val instanceof Object[])
            return (Object[])val;
        int arrayLength = java.lang.reflect.Array.getLength(val);
        Object[] outputArray = new Object[arrayLength];
        for(int i = 0; i < arrayLength; ++i){
            outputArray[i] = java.lang.reflect.Array.get(val, i);
        }
        return outputArray;
    }

    static abstract class ParameterSetter {
        // the number of parameter to set ; always equals to 1 except when working on an array parameter
        int parameterCount;

        public ParameterSetter() {
            this(1);
        }

        ParameterSetter(int parameterCount) {
            this.parameterCount = parameterCount;
        }

        abstract void setParameter(int paramIdx, PreparedStatement statement) throws SQLException;
    }

}
