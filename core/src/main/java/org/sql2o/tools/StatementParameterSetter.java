package org.sql2o.tools;

import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public interface StatementParameterSetter
{
    void setObject(String name, Object value) throws SQLException;

    void setNull(String name, int sqlType) throws SQLException;

    void setInputStream(String name, InputStream value) throws SQLException;

    void setString(String name, String value) throws SQLException;

    void setInt(String name, int value) throws SQLException;

    void setLong(String name, long value) throws SQLException;

    void setTimestamp(String name, Timestamp value) throws SQLException;

    void setDate(String name, Date value) throws SQLException;

    void setTime(String name, Time value) throws SQLException;
}
