package org.sql2o;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: dimzon
 * Date: 4/7/14
 * Time: 12:01 AM
 * @param <T> the type of the objects that this handler will return
 */
public interface ResultSetHandler<T> {
    T handle(ResultSet resultSet) throws SQLException;
}
