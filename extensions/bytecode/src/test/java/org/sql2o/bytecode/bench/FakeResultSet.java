package org.sql2o.bytecode.bench;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Фиктивный result set поверх готового массива строк: драйвер исключён полностью.
 *
 * <p>Строки — те же типы, что отдаёт драйвер (String, java.sql.Date, BigDecimal), поэтому
 * конвертеры делают ту же работу, что и на живой базе. Живых методов пять: next, close,
 * getObject, wasNull и getMetaData — остальное кидает UnsupportedOperationException, и если
 * маппинг туда полезет, это будет видно сразу, а не в виде странной цифры.
 */
final class FakeResultSet implements ResultSet {

    private final Object[][] rows;
    private final ResultSetMetaData meta;
    private int row = -1;
    private Object lastRead;

    FakeResultSet(Object[][] rows, ResultSetMetaData meta) {
        this.rows = rows;
        this.meta = meta;
    }

    @Override
    public boolean next() {
        row++;
        return row < rows.length;
    }

    @Override
    public void close() {
    }

    @Override
    public Object getObject(int column) {
        lastRead = rows[row][column - 1];
        return lastRead;
    }

    @Override
    public boolean wasNull() {
        return lastRead == null;
    }

    @Override
    public ResultSetMetaData getMetaData() {
        return meta;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("fake");
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        throw new UnsupportedOperationException("fake");
    }
    public java.lang.String getString(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean getBoolean(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public byte getByte(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public short getShort(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getInt(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public long getLong(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public float getFloat(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public double getDouble(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.math.BigDecimal getBigDecimal(int p0, int p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public byte[] getBytes(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Date getDate(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Time getTime(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Timestamp getTimestamp(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getAsciiStream(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getUnicodeStream(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getBinaryStream(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getString(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean getBoolean(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public byte getByte(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public short getShort(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getInt(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public long getLong(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public float getFloat(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public double getDouble(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.math.BigDecimal getBigDecimal(java.lang.String p0, int p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public byte[] getBytes(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Date getDate(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Time getTime(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Timestamp getTimestamp(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getAsciiStream(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getUnicodeStream(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.InputStream getBinaryStream(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.SQLWarning getWarnings() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void clearWarnings() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getCursorName() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.Object getObject(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int findColumn(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.Reader getCharacterStream(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.Reader getCharacterStream(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.math.BigDecimal getBigDecimal(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.math.BigDecimal getBigDecimal(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isBeforeFirst() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isAfterLast() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isFirst() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isLast() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void beforeFirst() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void afterLast() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean first() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean last() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean absolute(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean relative(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean previous() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void setFetchDirection(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getFetchDirection() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void setFetchSize(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getFetchSize() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getType() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getConcurrency() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean rowUpdated() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean rowInserted() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean rowDeleted() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNull(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBoolean(int p0, boolean p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateByte(int p0, byte p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateShort(int p0, short p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateInt(int p0, int p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateLong(int p0, long p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateFloat(int p0, float p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateDouble(int p0, double p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBigDecimal(int p0, java.math.BigDecimal p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateString(int p0, java.lang.String p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBytes(int p0, byte[] p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateDate(int p0, java.sql.Date p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateTime(int p0, java.sql.Time p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateTimestamp(int p0, java.sql.Timestamp p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(int p0, java.io.Reader p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(int p0, java.lang.Object p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(int p0, java.lang.Object p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNull(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBoolean(java.lang.String p0, boolean p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateByte(java.lang.String p0, byte p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateShort(java.lang.String p0, short p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateInt(java.lang.String p0, int p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateLong(java.lang.String p0, long p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateFloat(java.lang.String p0, float p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateDouble(java.lang.String p0, double p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBigDecimal(java.lang.String p0, java.math.BigDecimal p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateString(java.lang.String p0, java.lang.String p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBytes(java.lang.String p0, byte[] p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateDate(java.lang.String p0, java.sql.Date p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateTime(java.lang.String p0, java.sql.Time p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateTimestamp(java.lang.String p0, java.sql.Timestamp p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(java.lang.String p0, java.io.InputStream p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(java.lang.String p0, java.io.InputStream p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(java.lang.String p0, java.io.Reader p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(java.lang.String p0, java.lang.Object p1, int p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(java.lang.String p0, java.lang.Object p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void insertRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void deleteRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void refreshRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void cancelRowUpdates() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void moveToInsertRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void moveToCurrentRow() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Statement getStatement() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.Object getObject(int p0, java.util.Map<java.lang.String, java.lang.Class<?>> p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Ref getRef(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Blob getBlob(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Clob getClob(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Array getArray(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.Object getObject(java.lang.String p0, java.util.Map<java.lang.String, java.lang.Class<?>> p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Ref getRef(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Blob getBlob(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Clob getClob(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Array getArray(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Date getDate(int p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Date getDate(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Time getTime(int p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Time getTime(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Timestamp getTimestamp(int p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.Timestamp getTimestamp(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.net.URL getURL(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.net.URL getURL(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateRef(int p0, java.sql.Ref p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateRef(java.lang.String p0, java.sql.Ref p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(int p0, java.sql.Blob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(java.lang.String p0, java.sql.Blob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(int p0, java.sql.Clob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(java.lang.String p0, java.sql.Clob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateArray(int p0, java.sql.Array p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateArray(java.lang.String p0, java.sql.Array p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.RowId getRowId(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.RowId getRowId(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateRowId(int p0, java.sql.RowId p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateRowId(java.lang.String p0, java.sql.RowId p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public int getHoldability() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public boolean isClosed() throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNString(int p0, java.lang.String p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNString(java.lang.String p0, java.lang.String p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(int p0, java.sql.NClob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(java.lang.String p0, java.sql.NClob p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.NClob getNClob(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.NClob getNClob(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.SQLXML getSQLXML(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.sql.SQLXML getSQLXML(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateSQLXML(int p0, java.sql.SQLXML p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateSQLXML(java.lang.String p0, java.sql.SQLXML p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getNString(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.lang.String getNString(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.Reader getNCharacterStream(int p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public java.io.Reader getNCharacterStream(java.lang.String p0) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNCharacterStream(int p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNCharacterStream(java.lang.String p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(int p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(int p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(int p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(java.lang.String p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(java.lang.String p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(java.lang.String p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(int p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(java.lang.String p0, java.io.InputStream p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(int p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(java.lang.String p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(int p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(java.lang.String p0, java.io.Reader p1, long p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNCharacterStream(int p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNCharacterStream(java.lang.String p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(int p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(int p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(int p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateAsciiStream(java.lang.String p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBinaryStream(java.lang.String p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateCharacterStream(java.lang.String p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(int p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateBlob(java.lang.String p0, java.io.InputStream p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(int p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateClob(java.lang.String p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(int p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateNClob(java.lang.String p0, java.io.Reader p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public <T> T getObject(int p0, java.lang.Class<T> p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public <T> T getObject(java.lang.String p0, java.lang.Class<T> p1) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(int p0, java.lang.Object p1, java.sql.SQLType p2, int p3) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(java.lang.String p0, java.lang.Object p1, java.sql.SQLType p2, int p3) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(int p0, java.lang.Object p1, java.sql.SQLType p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
    public void updateObject(java.lang.String p0, java.lang.Object p1, java.sql.SQLType p2) throws java.sql.SQLException { throw new UnsupportedOperationException("fake"); }
}

