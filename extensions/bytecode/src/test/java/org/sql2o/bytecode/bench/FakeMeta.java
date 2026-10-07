package org.sql2o.bytecode.bench;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * The metadata of the fake result set: the column count and their names, and nothing else.
 *
 * <p>A factory reads metadata once, when it creates its handler, so there is nothing here to be slow about — what
 * matters is only that the names are the ones a driver would have reported.
 */
final class FakeMeta implements ResultSetMetaData {

    private final String[] labels;

    FakeMeta(String... labels) {
        this.labels = labels.clone();
    }

    @Override
    public int getColumnCount() {
        return labels.length;
    }

    @Override
    public String getColumnLabel(int column) {
        return labels[column - 1];
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("fake");
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        throw new UnsupportedOperationException("fake");
    }
    public boolean isAutoIncrement(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isCaseSensitive(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isSearchable(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isCurrency(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int isNullable(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isSigned(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getColumnDisplaySize(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getColumnName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getSchemaName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getPrecision(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getScale(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getTableName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getCatalogName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getColumnType(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getColumnTypeName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isReadOnly(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isWritable(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isDefinitelyWritable(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getColumnClassName(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
}
