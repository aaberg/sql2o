package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.quirks.Db2Quirks;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.PostgresQuirks;
import org.sql2o.quirks.Quirks;

import java.io.InputStream;
import java.sql.*;
import java.util.Map;

/**
 * Use {@link org.sql2o.quirks.Quirks}.
 */
@Deprecated
public enum QuirksMode {

    None(new NoQuirks()),
    DB2(new Db2Quirks()),
    PostgreSQL(new PostgresQuirks()),
    MSSqlServer(new NoQuirks());

    public final Quirks quirks;

    private QuirksMode(Quirks quirks) {
        this.quirks = quirks;
    }
}
