package org.sql2o;

/**
 * Many JDBC drivers has quirks and needs special treatment. This enum is used to specify which quirks sql2o should expect.
 */
public enum QuirksMode {

    None(true), DB2(true), PostgreSQL(false), MSSqlServer(true), Oracle(false);

    private final boolean returnGeneratedKeysByDefault;

    QuirksMode(boolean returnGeneratedKeysByDefault) {
        this.returnGeneratedKeysByDefault = returnGeneratedKeysByDefault;
    }

    public boolean isReturnGeneratedKeysByDefault() {
        return returnGeneratedKeysByDefault;
    }
}
