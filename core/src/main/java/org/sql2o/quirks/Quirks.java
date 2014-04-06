package org.sql2o.quirks;

import org.sql2o.converters.Converter;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * Interface for JDBC driver specific quirks.
 * See {@link org.sql2o.quirks.NoQuirks} for defaults.
 *
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public interface Quirks {

    /**
     * @return converters to register by default
     */
    Map<Class, Converter> customConverters();

    /**
     * @return name of column at index {@code colIdx} for result set {@code meta}
     */
    String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException;

    /**
     * @return true if queries should return generated keys by default, false otherwise
     */
    boolean returnGeneratedKeysByDefault();
}
