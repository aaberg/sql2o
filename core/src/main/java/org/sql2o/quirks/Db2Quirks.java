package org.sql2o.quirks;

import org.sql2o.converters.Converter;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class Db2Quirks extends NoQuirks {
    // Db2 works perfect with java.sql.Timestamp
    // checked on DATE|TIME|TIMESTAMP column types

    @Override
    public String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
        return meta.getColumnName(colIdx);
    }
}
