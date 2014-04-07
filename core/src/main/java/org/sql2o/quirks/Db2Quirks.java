package org.sql2o.quirks;

import org.sql2o.converters.Converter;
import org.sql2o.converters.DateConverterNoTimestamp;

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

    public Map<Class, Converter> customConverters() {
        Map<Class, Converter> customConverters = new HashMap<Class, Converter>();

        Converter dateConverter = new DateConverterNoTimestamp();
        customConverters.put(Date.class, dateConverter);
        customConverters.put(java.sql.Date.class, dateConverter);
        customConverters.put(java.sql.Time.class, dateConverter);
        customConverters.put(java.sql.Timestamp.class, dateConverter);

        return customConverters;
    }

    @Override
    public String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
        return meta.getColumnName(colIdx);
    }
}
