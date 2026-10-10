package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.quirks.NoQuirks;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PojoResultSetIterator}, which delegates the reading of each row to a handler.
 */
public class PojoResultSetIteratorTest {

    private static final String[] ROWS = {"first", "second"};

    private static ResultSet resultSetWithTwoRows() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(mock(ResultSetMetaData.class));
        when(rs.next()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        when(rs.getString(1)).thenReturn(ROWS[0], ROWS[1]);
        return rs;
    }

    private static ResultSetHandler<String> echoHandler() {
        return rs -> "handled " + rs.getString(1);
    }

    @Test
    public void everyRowGoesThroughTheHandler() throws Exception {
        final ResultSet rs = resultSetWithTwoRows();
        final PojoResultSetIterator<String> it =
                new PojoResultSetIterator<>(rs, false, new NoQuirks(), echoHandler());

        assertTrue(it.hasNext());
        assertEquals("handled first", it.next());
        assertEquals("handled second", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void theHandlerIsGivenTheSameResultSetOnEveryRow() throws Exception {
        final ResultSet rs = resultSetWithTwoRows();
        final PojoResultSetIterator<String> it =
                new PojoResultSetIterator<>(rs, false, new NoQuirks(), echoHandler());

        it.next();
        it.next();

        verify(rs, times(2)).getString(1);
    }

    /**
     * A handler can also be built from the metadata through a factory. The factory is spelled out as an anonymous
     * class on purpose: both constructors take a single method interface of the same shape, so a lambda here does
     * not compile.
     */
@Test
    public void aHandlerCanBeBuiltFromTheMetadataThroughAFactory() throws Exception {
        final ResultSet rs = resultSetWithTwoRows();
        final PojoResultSetIterator<String> it = new PojoResultSetIterator<>(rs, false, new NoQuirks(),
                new ResultSetHandlerFactory<String>() {
                    @Override
                    public ResultSetHandler<String> newResultSetHandler(ResultSetMetaData meta) {
                        return resultSet -> "from factory: " + resultSet.getString(1);
                    }
                });

        assertEquals("from factory: first", it.next());
    }

    /**
     * The metadata is asked for twice, once by the base class and once for the handler factory, so a failure on the
     * second ask is what reaches this constructor.
     */
    @Test
    public void metadataThatFailsForTheHandlerFactoryIsReported() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(mock(ResultSetMetaData.class)).thenThrow(new SQLException("gone"));

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> new PojoResultSetIterator<String>(
                rs, false, new NoQuirks(), new ResultSetHandlerFactory<String>() {
                    @Override
                    public ResultSetHandler<String> newResultSetHandler(ResultSetMetaData meta) {
                        return echoHandler();
                    }
                }));

        assertEquals("Database error: gone", ex.getMessage());
        assertTrue(ex.getCause() instanceof SQLException);
    }
}