package org.sql2o.quirks;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class PostgresQuirks extends NoQuirks {
    @Override
    public boolean returnGeneratedKeysByDefault() {
        return false;
    }
}
