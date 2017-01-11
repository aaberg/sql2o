package org.sql2o.connectionsources;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Simple {@link Connection wrapper}
 * Created by nickl on 09.01.17.
 */
public class WrappedConnection implements Connection {


    private Connection source;


    public WrappedConnection(Connection source) {
        this.source = source;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return source.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return source.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return source.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return source.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        source.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return source.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        source.commit();
    }

    @Override
    public void rollback() throws SQLException {
        source.rollback();
    }

    @Override
    public void close() throws SQLException {
        source.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return source.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return source.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        source.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return source.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        source.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return source.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        source.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return source.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return source.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        source.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return source.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return source.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return source.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return source.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        source.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        source.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return source.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return source.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return source.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        source.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        source.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return source.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return source.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return source.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return source.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return source.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return source.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return source.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return source.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return source.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return source.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return source.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        source.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        source.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return source.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return source.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return source.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return source.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        source.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return source.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        source.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        source.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return source.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return source.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return source.isWrapperFor(iface);
    }
}
