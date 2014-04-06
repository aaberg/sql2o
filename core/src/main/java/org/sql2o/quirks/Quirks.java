package org.sql2o.quirks;

import org.sql2o.converters.Converter;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public interface Quirks {

    Map<Class, Converter> customConverters();

    String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException;

    boolean returnGeneratedKeysByDefault();
}
