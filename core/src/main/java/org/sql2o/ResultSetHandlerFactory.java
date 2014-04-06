package org.sql2o;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * User: dimzon
 * Date: 4/7/14
 * Time: 12:02 AM
 */
public interface ResultSetHandlerFactory {
    <T> ResultSetHandler<T> newResultSetHandler(Class<T> type, ResultSetMetaData resultSetMetaData) throws SQLException;
}
