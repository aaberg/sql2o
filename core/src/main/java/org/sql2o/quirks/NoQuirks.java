package org.sql2o.quirks;

import org.sql2o.converters.Converter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class NoQuirks implements Quirks {
    public Map<Class, Converter> customConverters() {
        return Collections.emptyMap();
    }

    public String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
        return meta.getColumnLabel(colIdx);
    }

    public boolean returnGeneratedKeysByDefault() {
        return true;
    }

    public Object getRSVal(ResultSet rs, int idx) throws SQLException {
        return rs.getObject(idx);
    }
}
