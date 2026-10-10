package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.quirks.NoQuirks;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ResultSetIteratorBase}, the iteration protocol the other iterators build on.
 *
 * <p>The contract that matters is that hasNext() may be called any number of times and still leave the row it peeked
 * at there for next(), and that once the result set is exhausted it stays exhausted.
 */
public class ResultSetIteratorBaseTest {

    /** Counts how often the result set was asked for the next row. */
    private static class CountingIterator extends ResultSetIteratorBase<String> {

        private final boolean throwOnRead;

        private CountingIterator(ResultSet rs, boolean throwOnRead) {
            super(rs, false, new NoQuirks());
            this.throwOnRead = throwOnRead;
        }

        @Override
        protected String readNext() throws SQLException {
            if (throwOnRead) {
                throw new SQLException("cannot read this row");
            }
            return rs.getString(1);
        }
    }

    /** A result set that reports exactly rowCount rows and then runs out. */
    private static ResultSet resultSetWithRows(int rowCount) throws SQLException {
        final Boolean[] nextAnswers = new Boolean[rowCount + 1];
        final String[] values = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            nextAnswers[i] = Boolean.TRUE;
            values[i] = "row " + (i + 1);
        }
        nextAnswers[rowCount] = Boolean.FALSE;

        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.next()).thenReturn(nextAnswers[0], Arrays.copyOfRange(nextAnswers, 1, nextAnswers.length));
        if (rowCount > 0) {
            when(rs.getString(1)).thenReturn(values[0], Arrays.copyOfRange(values, 1, values.length));
        }
        return rs;
    }

    private static CountingIterator iterator(ResultSet rs) {
        return new CountingIterator(rs, false);
    }

    @Test
    public void everyRowIsHandedOutInOrder() throws Exception {
        final CountingIterator it = iterator(resultSetWithRows(3));

        assertTrue(it.hasNext());
        assertEquals("row 1", it.next());
        assertTrue(it.hasNext());
        assertEquals("row 2", it.next());
        assertTrue(it.hasNext());
        assertEquals("row 3", it.next());
        assertFalse(it.hasNext());
    }

    /** hasNext() has to be repeatable without eating the row it looked at. */
    @Test
    public void hasNextCanBeCalledRepeatedlyWithoutLosingTheRow() throws Exception {
        final ResultSet rs = resultSetWithRows(1);
        final CountingIterator it = iterator(rs);

        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());

        assertEquals("row 1", it.next());
        verify(rs, times(1)).next();
    }

    @Test
    public void nextCanBeCalledWithoutHasNext() throws Exception {
        final CountingIterator it = iterator(resultSetWithRows(2));

        assertEquals("row 1", it.next());
        assertEquals("row 2", it.next());
        assertFalse(it.hasNext());
    }

    /** Once the result set ran out, asking again must not query it again either. */
    @Test
    public void anExhaustedIteratorStaysExhaustedWithoutTouchingTheResultSet() throws Exception {
        final ResultSet rs = resultSetWithRows(1);
        final CountingIterator it = iterator(rs);

        assertEquals("row 1", it.next());
        // the row was fetched by next(), and noticing that there is nothing left costs one more call
        assertFalse(it.hasNext());
        verify(rs, times(2)).next();

        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
        verify(rs, times(2)).next();
    }

    @Test
    public void askingPastTheEndIsAnError() throws Exception {
        final CountingIterator it = iterator(resultSetWithRows(1));

        assertEquals("row 1", it.next());

        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void anEmptyResultSetHasNothingAtAll() throws Exception {
        final CountingIterator it = iterator(resultSetWithRows(0));

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void removeIsNotSupported() throws Exception {
        final CountingIterator it = iterator(resultSetWithRows(1));

        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    public void metadataThatCannotBeReadIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenThrow(new SQLException("closed"));

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> iterator(rs));

        assertEquals("Database error: closed", ex.getMessage());
        assertTrue(ex.getCause() instanceof SQLException);
    }

    @Test
    public void aResultSetThatFailsOnNextIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(mock(ResultSetMetaData.class));
        when(rs.next()).thenThrow(new SQLException("connection lost"));

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> iterator(rs).hasNext());

        assertEquals("Database error: connection lost", ex.getMessage());
    }

    @Test
    public void aRowThatFailsToReadIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(mock(ResultSetMetaData.class));
        when(rs.next()).thenReturn(true, false);

        final Sql2oException ex =
                assertThrows(Sql2oException.class, () -> new CountingIterator(rs, true).hasNext());

        assertEquals("Database error: cannot read this row", ex.getMessage());
    }

    @Test
    public void theColumnNameComesFromTheQuirks() throws Exception {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(meta);

        final ResultSetIteratorBase<String> it = new ResultSetIteratorBase<String>(rs, false, new NoQuirks() {
            @Override
            public String getColumnName(ResultSetMetaData m, int colIdx) {
                return "quirks column " + colIdx;
            }
        }) {
            @Override
            protected String readNext() {
                return "unused";
            }
        };

        assertEquals("quirks column 2", it.getColumnName(2));
    }
}