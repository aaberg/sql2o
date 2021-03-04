package org.sql2o;

import java.io.Closeable;

/**
 * Iterable {@link java.sql.ResultSet}. Needs to be closeable, because allowing manual
 * iteration means it's impossible to know when to close the ResultSet and Connection.
 *
 * @author aldenquimby@gmail.com
 */
public interface ResultSetIterable<T> extends Iterable<T>, Closeable, AutoCloseable {
    // override close to not throw
    @Override
    void close();

    boolean isAutoCloseConnection();
    void setAutoCloseConnection(boolean autoCloseConnection);
}
