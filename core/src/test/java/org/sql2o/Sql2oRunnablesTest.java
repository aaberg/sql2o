package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.connectionsources.ConnectionSource;
import org.sql2o.connectionsources.DataSourceConnectionSource;
import org.sql2o.quirks.QuirksDetector;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Sql2o} around connection sources and the runnable callbacks.
 *
 * <p>These are the paths where Sql2o owns the connection, so what matters is that it is always closed and that a
 * failure inside the callback comes back wrapped rather than raw.
 */
public class Sql2oRunnablesTest {

    private static final String URL = "jdbc:h2:mem:sql2o_runnables_test";

    private static Sql2o sql2o() {
        return new Sql2o(URL, "sa", "");
    }

    /** A Sql2o whose connections are mocked, so they can be made to misbehave. */
    private static Sql2o sql2oOver(java.sql.Connection jdbc) {
        final Sql2o sql2o = sql2o();
        sql2o.setConnectionSource(() -> jdbc);
        return sql2o;
    }

    private static java.sql.Connection quietJdbc() throws SQLException {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        when(jdbc.getAutoCommit()).thenReturn(true);
        return jdbc;
    }

    /**
     * A connection that records the three calls sql2o makes on the connection it opened itself, so the cleanup can be
     * asserted on. Mockito does the bookkeeping, which saves implementing fifty methods of the interface.
     */
    private static java.sql.Connection recordingJdbc(AtomicBoolean closed, AtomicBoolean committed,
            AtomicBoolean rolledBack) throws SQLException {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenAnswer(invocation -> closed != null && closed.get());
        when(jdbc.getAutoCommit()).thenReturn(false);
        org.mockito.Mockito.doAnswer(invocation -> {
            if (closed != null) {
                closed.set(true);
            }
            return null;
        }).when(jdbc).close();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (committed != null) {
                committed.set(true);
            }
            return null;
        }).when(jdbc).commit();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (rolledBack != null) {
                rolledBack.set(true);
            }
            return null;
        }).when(jdbc).rollback();
        return jdbc;
    }

    @Test
    public void theDataSourceIsHandedOutWhenThereIsOne() {
        final Sql2o sql2o = sql2o();
        final DataSource dataSource = ((DataSourceConnectionSource) sql2o.getConnectionSource()).getDataSource();

        assertSame(dataSource, sql2o.getDataSource());
    }

    /** The deprecated accessor has nothing to answer when the connections do not come from a DataSource. */
    @Test
    public void theDeprecatedDataSourceAccessorIsEmptyForAnyOtherSource() throws Exception {
        final Sql2o sql2o = sql2oOver(quietJdbc());

        assertNull(sql2o.getDataSource());
    }

    @Test
    public void theConnectionSourceCanBeReplaced() throws Exception {
        final Sql2o sql2o = sql2o();
        final ConnectionSource replacement = () -> quietJdbc();

        sql2o.setConnectionSource(replacement);

        assertSame(replacement, sql2o.getConnectionSource());
    }

    @Test
    public void withConnectionRunsAndClosesTheConnection() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(closed, null, null));

        final String result = sql2o.withConnection((con, argument) -> {
            assertEquals("argument", argument);
            return "done";
        }, "argument");

        assertEquals("done", result);
        assertTrue(closed.get(), "the connection sql2o opened has to be closed again");
    }

    @Test
    public void withConnectionReportsAFailureFromTheCallback() throws Exception {
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), null, null));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> sql2o.withConnection((StatementRunnable) (con, arg) -> {
                    throw new IllegalStateException("callback failed");
                }));

        assertEquals("An error occurred while executing StatementRunnable", ex.getMessage());
        assertEquals(IllegalStateException.class, ex.getCause().getClass());
    }

    /** Even a failing callback must not leave the connection open. */
    @Test
    public void withConnectionClosesTheConnectionAfterAFailure() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(closed, null, null));

        assertThrows(Sql2oException.class, () -> sql2o.withConnection((StatementRunnable) (con, arg) -> {
            throw new IllegalStateException("callback failed");
        }));

        assertTrue(closed.get());
    }

    @Test
    public void withConnectionWithoutAnArgumentWorksToo() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(closed, null, null));

        assertEquals(7, sql2o.withConnection((StatementRunnableWithResult<Integer>) (con, arg) -> 7));
        assertTrue(closed.get());
    }

    /**
     * StatementRunnable and StatementRunnableWithResult have the same shape, so a lambda that only throws fits both
     * and has to be cast by hand. Noted here because it is what the two overloads cost the caller.
     */
    @Test
    public void aCallbackThatOnlyThrowsNeedsTheInterfaceSpelledOut() throws Exception {
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), null, null));

        assertThrows(Sql2oException.class, () -> sql2o.withConnection((StatementRunnable) (con, arg) -> {
            throw new IllegalStateException("callback failed");
        }));
    }

    @Test
    public void aVoidCallbackRunsAndClosesTheConnection() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicBoolean ran = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(closed, null, null));

        sql2o.withConnection((StatementRunnable) (con, argument) -> ran.set(true), "argument");

        assertTrue(ran.get());
        assertTrue(closed.get());
    }

    @Test
    public void aVoidCallbackReportsAFailure() throws Exception {
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), null, null));

        assertThrows(Sql2oException.class, () -> sql2o.withConnection((StatementRunnable) (con, arg) -> {
            throw new IllegalStateException("callback failed");
        }));
    }

    @Test
    public void aTransactionIsPreparedAndHandedOut() throws Exception {
        final java.sql.Connection jdbc = quietJdbc();
        final Sql2o sql2o = sql2oOver(jdbc);

        try (Connection con = sql2o.beginTransaction()) {
            org.mockito.Mockito.verify(jdbc).setAutoCommit(false);
            org.mockito.Mockito.verify(jdbc).setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
        }
    }

    @Test
    public void aTransactionThatCannotBeStartedIsReportedAndTheConnectionClosed() throws Exception {
        final java.sql.Connection jdbc = quietJdbc();
        doThrow(new SQLException("no transactions here")).when(jdbc).setTransactionIsolation(anyInt());
        final Sql2o sql2o = sql2oOver(jdbc);

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> sql2o.beginTransaction());

        assertEquals("Could not start the transaction - no transactions here", ex.getMessage());
        verifyClosed(jdbc);
    }

    @Test
    public void runInTransactionCommitsWhenTheCallbackSucceeds() throws Exception {
        final AtomicBoolean committed = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), committed, null));

        final String result = sql2o.runInTransaction((StatementRunnableWithResult<String>) (con, arg) -> {
            assertNull(arg);
            return "result";
        });

        assertEquals("result", result);
        assertTrue(committed.get());
    }

    @Test
    public void runInTransactionRollsBackAndReportsWhenTheCallbackFails() throws Exception {
        final AtomicBoolean rolledBack = new AtomicBoolean();
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), null, rolledBack));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> sql2o.runInTransaction((StatementRunnableWithResult<String>) (con, arg) -> {
                    throw new IllegalStateException("callback failed");
                }));

        assertEquals("An error occurred while executing StatementRunnableWithResult. Transaction rolled back.",
                ex.getMessage());
        assertTrue(rolledBack.get());
    }

    @Test
    public void runInTransactionTakesAnArgumentAndAnIsolationLevel() throws Exception {
        final java.sql.Connection jdbc = quietJdbc();
        final Sql2o sql2o = sql2oOver(jdbc);

        final String result = sql2o.runInTransaction(
                (StatementRunnableWithResult<String>) (con, arg) -> String.valueOf(arg),
                "given", java.sql.Connection.TRANSACTION_SERIALIZABLE);

        assertEquals("given", result);
    }

    @Test
    public void withConnectionWithResultAndArgumentReportsAFailure() throws Exception {
        final Sql2o sql2o = sql2oOver(recordingJdbc(new AtomicBoolean(), null, null));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> sql2o.withConnection((StatementRunnableWithResult<String>) (con, arg) -> {
                    throw new IllegalStateException("callback failed");
                }, "argument"));

        assertEquals("An error occurred while executing StatementRunnable", ex.getMessage());
    }

    /** When opening the connection fails there is nothing to close, and the finally block has to cope with that. */
    @Test
    public void aConnectionThatCannotBeOpenedLeavesNothingToClose() throws Exception {
        final Sql2o sql2o = sql2o();
        sql2o.setConnectionSource(() -> {
            throw new SQLException("pool exhausted");
        });

        assertThrows(Sql2oException.class, () -> sql2o.withConnection((StatementRunnable) (con, arg) -> {
        }));
        assertThrows(Sql2oException.class, () -> sql2o.withConnection(
                (StatementRunnableWithResult<String>) (con, arg) -> "never"));
    }

    @Test
    public void theQuirksAreTheOnesTheUrlAsksFor() {
        // a fresh instance is handed out per call, so compare the kind rather than the identity
        assertInstanceOf(org.sql2o.quirks.H2Quirks.class, sql2o().getQuirks());
        assertInstanceOf(org.sql2o.quirks.H2Quirks.class, QuirksDetector.forURL(URL));
    }

    private static void verifyClosed(java.sql.Connection jdbc) throws SQLException {
        org.mockito.Mockito.verify(jdbc).close();
    }
}