package org.sql2o.bytecode.bench;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Метаданные фиктивного result set: только число колонок и их имена.
 *
 * <p>Фабрика читает метаданные один раз, при создании хэндлера, поэтому здесь нечему тормозить —
 * важно лишь, что имена совпадают с теми, что видел бы драйвер.
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

