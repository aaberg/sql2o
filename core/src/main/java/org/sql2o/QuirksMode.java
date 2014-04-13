package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.quirks.Db2Quirks;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.PostgresQuirks;
import org.sql2o.quirks.Quirks;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * Use {@link org.sql2o.quirks.Quirks}.
 */
@Deprecated
public enum QuirksMode implements Quirks {

    None(new NoQuirks()),
    DB2(new Db2Quirks()),
    PostgreSQL(new PostgresQuirks()),
    MSSqlServer(new NoQuirks());

    private final Quirks quirks;

    public Map<Class, Converter> customConverters() {
        return quirks.customConverters();
    }

    public String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
        return quirks.getColumnName(meta, colIdx);
    }

    public boolean returnGeneratedKeysByDefault() {
        return quirks.returnGeneratedKeysByDefault();
    }

    private QuirksMode(Quirks quirks) {
        this.quirks = quirks;
    }
}
