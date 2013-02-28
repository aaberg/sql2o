package org.sql2o;

/**
 * Many JDBC drivers has quirks and needs special treatment. This enum is used to specify which quirks sql2o should expect.
 */
public enum QuirksMode {

    None, DB2, PostgreSQL
}
