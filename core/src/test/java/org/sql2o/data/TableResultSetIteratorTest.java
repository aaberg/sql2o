package org.sql2o.data;

import org.junit.jupiter.api.Test;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.NoQuirks;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TableResultSetIterator}, which turns result set metadata into columns and rows.
 *
 * <p>The interesting decision here is what happens to the column names: a case sensitive result set keeps them as the
 * database spelled them, anything else lowercases them so that a lookup cannot be case sensitive by accident.
 */
public class TableResultSetIteratorTest {

    private static final String[] COLUMN_NAMES = {"NAME", "AGE"};
    private static final String[] COLUMN_TYPES = {"VARCHAR", "INTEGER"};

    private static ResultSet resultSetWithTwoColumns() throws SQLException {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getTableName(1)).thenReturn("MY_TABLE");
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn(COLUMN_NAMES[0]);
        when(meta.getColumnTypeName(1)).thenReturn(COLUMN_TYPES[0]);
        when(meta.getColumnLabel(2)).thenReturn(COLUMN_NAMES[1]);
        when(meta.getColumnTypeName(2)).thenReturn(COLUMN_TYPES[1]);

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(meta);
        return rs;
    }

    private static TableResultSetIterator iterator(ResultSet rs, boolean caseSensitive, LazyTable table) {
        return new TableResultSetIterator(rs, caseSensitive, new NoQuirks(), table);
    }

    @Test
    public void theTableNameAndColumnsAreTakenFromTheMetadata() throws Exception {
        final LazyTable table = new LazyTable();
        final TableResultSetIterator it = iterator(resultSetWithTwoColumns(), false, table);

        final List<Column> columns = table.columns();

        assertEquals("MY_TABLE", table.getName());
        assertEquals(2, columns.size());
        assertEquals("NAME", columns.get(0).getName());
        assertEquals("INTEGER", columns.get(1).getType());
    }

    /** Column indexes are zero based here, while the metadata is one based. */
    @Test
    public void columnIndexesAreCountedFromZero() throws Exception {
        final LazyTable table = new LazyTable();
        final TableResultSetIterator it = iterator(resultSetWithTwoColumns(), false, table);

        assertEquals(0, table.columns().get(0).getIndex());
        assertEquals(1, table.columns().get(1).getIndex());
        assertEquals(2, table.columns().size());
    }

    @Test
    public void aRowCanBeReadBackByTheLowercasedColumnName() throws Exception {
        final LazyTable table = new LazyTable();
        final ResultSet rs = resultSetWithTwoColumns();
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("Alice");
        when(rs.getObject(2)).thenReturn(30);

        final TableResultSetIterator it = iterator(rs, false, table);

        assertTrue(it.hasNext());
        final Row row = it.next();

        assertEquals("Alice", row.getObject("name"));
        assertEquals(30, row.getObject("age"));
        assertEquals("Alice", row.getObject("NAME"));
    }

    @Test
    public void aCaseSensitiveResultSetKeepsTheColumnNameAsSpelled() throws Exception {
        final LazyTable table = new LazyTable();
        final ResultSet rs = resultSetWithTwoColumns();
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("Alice");
        when(rs.getObject(2)).thenReturn(30);

        final TableResultSetIterator it = iterator(rs, true, table);
        final Row row = it.next();

        assertEquals("Alice", row.getObject("NAME"));
        assertThrows(Sql2oException.class, () -> row.getObject("name"));
    }

    @Test
    public void metadataThatCannotBeReadIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getTableName(1)).thenThrow(new SQLException("no metadata for you"));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> new TableResultSetIterator(rs, false, new NoQuirks(), new LazyTable()));

        assertEquals("Error while reading metadata from database", ex.getMessage());
        assertTrue(ex.getCause() instanceof SQLException);
    }

    @Test
    public void aResultSetThatCannotBeAskedForItsMetadataIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenThrow(new SQLException("closed"));

        assertThrows(Sql2oException.class,
                () -> new TableResultSetIterator(rs, false, new NoQuirks(), new LazyTable()));
    }

    @Test
    public void valuesAreReadThroughTheQuirks() throws Exception {
        final LazyTable table = new LazyTable();
        final ResultSet rs = resultSetWithTwoColumns();
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(2)).thenReturn("thirty");

        final TableResultSetIterator it = new TableResultSetIterator(rs, false, new NoQuirks() {
            @Override
            public Object getRSVal(ResultSet rs, int idx) throws SQLException {
                return "quirks " + idx;
            }
        }, table);

        final Row row = it.next();

        assertEquals("quirks 1", row.getObject("name"));
        assertEquals("quirks 2", row.getObject("age"));
    }
}