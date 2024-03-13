package org.sql2o;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * User: dimzon
 * Date: 4/7/14
 * Time: 12:02 AM
 * @param <T> the type of the objects that this handler will return
 */
public interface ResultSetHandlerFactory<T> {
    ResultSetHandler<T> newResultSetHandler(ResultSetMetaData resultSetMetaData) throws SQLException;
}
