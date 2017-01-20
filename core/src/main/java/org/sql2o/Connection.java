package org.sql2o;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Represents a connection to the database with additional feautures provided by Sql2o.
 */
public interface Connection extends AutoCloseable, Closeable {
    boolean isRollbackOnException();

    Connection setRollbackOnException(boolean rollbackOnException);

    boolean isRollbackOnClose();

    Connection setRollbackOnClose(boolean rollbackOnClose);

    java.sql.Connection getJdbcConnection();

    Settings getSettings();

    Query createQuery(String queryText);

    Query createQuery(String queryText, boolean returnGeneratedKeys);

    Query createQuery(String queryText, String... columnNames);

    Query createQueryWithParams(String queryText, Object... paramValues);

    void rollback();

    Connection rollback(boolean closeConnection);

    void commit();

    Connection commit(boolean closeConnection);

    int getResult();

    int[] getBatchResult();

    Object getKey();

    @SuppressWarnings("unchecked") // need to change Convert
    <V> V getKey(Class returnType);

    Object[] getKeys();

    @SuppressWarnings("unchecked") // need to change Convert
    <V> List<V> getKeys(Class<V> returnType);

    void close();
}
