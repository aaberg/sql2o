package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.connectionsources.ConnectionSource;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Connection} where the JDBC connection underneath it misbehaves.
 *
 * <p>The happy paths are covered by the tests that talk to H2; what is left here is everything that only happens when
 * the driver says no, which is where the error messages and the cleanup live.
 */
public class ConnectionTest {

    private static Sql2o sql2o() {
        // the three argument form is the url one; the single argument form is a JNDI lookup
        return new Sql2o("jdbc:h2:mem:connection_test", "sa", "");
    }

    private static Connection connectionOver(java.sql.Connection jdbc) {
        final ConnectionSource source = () -> jdbc;
        return new Connection(sql2o(), source, false);
    }

    private static java.sql.Connection quietJdbcConnection() throws SQLException {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        when(jdbc.getAutoCommit()).thenReturn(true);
        return jdbc;
    }

    @Test
    public void rollbackOnCloseIsOnByDefaultAndCanBeTurnedOff() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        assertTrue(connection.isRollbackOnClose());
        assertSame(connection, connection.setRollbackOnClose(false));
        assertFalse(connection.isRollbackOnClose());
    }

    @Test
    public void rollbackOnExceptionIsOnByDefaultAndCanBeTurnedOff() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        assertTrue(connection.isRollbackOnException());
        assertSame(connection, connection.setRollbackOnException(false));
        assertFalse(connection.isRollbackOnException());
    }

    @Test
    public void aQueryOnAClosedConnectionAsksTheSourceForANewOne() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        when(jdbc.isClosed()).thenReturn(true, false);
        final AtomicInteger acquisitions = new AtomicInteger();
        final ConnectionSource source = () -> {
            acquisitions.incrementAndGet();
            return jdbc;
        };
        final Connection connection = new Connection(sql2o(), source, false);

        connection.createQuery("select 1");

        assertEquals(2, acquisitions.get(), "one connection for the constructor and one for the closed one");
    }

    @Test
    public void aConnectionThatCannotSayWhetherItIsClosedIsReported() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenThrow(new SQLException("no idea"));
        final Connection connection = connectionOver(jdbc);

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> connection.createQuery("select 1"));

        assertEquals("Error creating connection", ex.getMessage());
    }

    /** The same two outcomes as the single argument overload, which is why both are worth a test. */
    @Test
    public void aNamedColumnQueryBehavesTheSameWay() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        final Connection connection = connectionOver(jdbc);

        connection.createQuery("insert into t values (?)", "id");

        final java.sql.Connection broken = mock(java.sql.Connection.class);
        when(broken.isClosed()).thenThrow(new SQLException("no idea"));
        final Connection brokenConnection = connectionOver(broken);

        assertThrows(Sql2oException.class, () -> brokenConnection.createQuery("insert into t values (?)", "id"));
    }

    @Test
    public void aTransactionLevelIsSetWithAutocommitTurnedOffAgain() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        final Connection connection = connectionOver(jdbc);

        connection.prepareForTransaction(java.sql.Connection.TRANSACTION_SERIALIZABLE);

        verify(jdbc).setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
        verify(jdbc, times(1)).setAutoCommit(false);
    }

    /** Autocommit has to be lifted first, otherwise the isolation level cannot be changed. */
    @Test
    public void autocommitIsForcedOnBeforeAnIsolationLevelIsSet() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        when(jdbc.getAutoCommit()).thenReturn(false);
        final Connection connection = connectionOver(jdbc);

        connection.prepareForTransaction(java.sql.Connection.TRANSACTION_READ_COMMITTED);

        verify(jdbc).setAutoCommit(true);
        verify(jdbc).setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
        verify(jdbc).setAutoCommit(false);
    }

    @Test
    public void aRollbackThatFailsIsOnlyWarnedAbout() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        when(jdbc.getAutoCommit()).thenReturn(false);
        doThrow(new SQLException("cannot roll back")).when(jdbc).rollback();
        final Connection connection = connectionOver(jdbc);

        connection.rollback(false);

        verify(jdbc).rollback();
    }

    @Test
    public void aCommitThatFailsIsReported() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        doThrow(new SQLException("cannot commit")).when(jdbc).commit();
        final Connection connection = connectionOver(jdbc);

        assertThrows(Sql2oException.class, () -> connection.commit(false));
    }

    @Test
    public void aSuccessfulCommitAnswersTheConnectionItself() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        assertSame(connection, connection.commit(false));
    }

    @Test
    public void askingForAResultBeforeExecutingSaysSoFirst() {
        final Sql2oException ex = assertThrows(Sql2oException.class, () -> connectionOver(quietJdbcConnectionUnchecked()).getResult());

        assertEquals("It is required to call executeUpdate() method before calling getResult().", ex.getMessage());
    }

    @Test
    public void theResultIsWhatTheLastExecutionLeftBehind() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        connection.setResult(4);

        assertEquals(4, connection.getResult());
    }

    @Test
    public void askingForABatchResultBeforeExecutingSaysSoFirst() throws Exception {
        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> connectionOver(quietJdbcConnectionUnchecked()).getBatchResult());

        assertEquals("It is required to call executeBatch() method before calling getBatchResult().", ex.getMessage());
    }

    @Test
    public void theBatchResultIsWhatTheLastBatchLeftBehind() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        connection.setBatchResult(new int[] {1, 2});

        assertEquals(2, connection.getBatchResult().length);
    }

    private static java.sql.Connection quietJdbcConnectionUnchecked() {
        try {
            return quietJdbcConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void keysCannotBeAskedForUnlessTheyWereFetched() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        assertThrows(Sql2oException.class, connection::getKey);
        assertThrows(Sql2oException.class, connection::getKeys);
        assertThrows(Sql2oException.class, () -> connection.getKeys(String.class));
    }

    @Test
    public void closingClosesTheStatementsAndThenTheConnection() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        final Statement statement = mock(Statement.class);
        final Connection connection = connectionOver(jdbc);
        connection.registerStatement(statement);

        connection.close();

        verify(statement).close();
        verify(jdbc).close();
    }

    /** A statement that refuses to close must not keep the connection from being closed. */
    @Test
    public void aStatementThatFailsToCloseIsOnlyWarnedAbout() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        final Statement statement = mock(Statement.class);
        doThrow(new SQLException("cannot close")).when(statement).close();
        final Connection connection = connectionOver(jdbc);
        connection.registerStatement(statement);

        connection.close();

        verify(jdbc).close();
    }

    @Test
    public void aConnectionThatCannotSayWhetherItIsClosedOnCloseIsReported() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenThrow(new SQLException("no idea"));
        final Connection connection = connectionOver(jdbc);

        final Sql2oException ex = assertThrows(Sql2oException.class, connection::close);

        assertEquals("Sql2o encountered a problem while trying to determine whether the connection is closed.",
                ex.getMessage());
    }

    @Test
    public void aConnectionThatFailsToCloseIsOnlyWarnedAbout() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        doThrow(new SQLException("cannot close")).when(jdbc).close();
        final Connection connection = connectionOver(jdbc);

        connection.close();
    }

    /** The auto commit mode the connection was handed over in is restored before it goes back to a pool. */
    @Test
    public void theOriginalAutoCommitModeIsRestoredOnClose() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        when(jdbc.getAutoCommit()).thenReturn(true);
        final Connection connection = connectionOver(jdbc);

        connection.close();

        verify(jdbc).setAutoCommit(true);
    }

    @Test
    public void aConnectionThatCannotBeAcquiredIsReported() {
        final ConnectionSource broken = () -> {
            throw new SQLException("pool exhausted");
        };

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> new Connection(sql2o(), broken, false));

        assertEquals("Could not acquire a connection from DataSource - pool exhausted", ex.getMessage());
    }

@Test
    public void theTypedKeyListIsEmptyWhenTheKeysAreOnlyAbsent() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());
        connection.setCanGetKeys(true);
        connection.setKeys(null);

        // a key list that was never filled is absent rather than empty, which the typed accessor answers as null
        assertNull(connection.getKeys(String.class));
    }

    @Test
    public void aQueryCanBeBuiltWithItsParameters() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());

        assertNotNull(connection.createQueryWithParams("select * from t where a = :p1 and b = :p2", 1, "two"));
    }

    /** Without the rollback on close the connection is simply closed, so the rollback branch is skipped. */
    @Test
    public void aConnectionAskedNotToRollBackIsOnlyClosed() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        final Connection connection = connectionOver(jdbc);
        connection.setRollbackOnClose(false);

        connection.close();

        verify(jdbc, never()).rollback();
        verify(jdbc).close();
    }

    /** A result set without rows still counts as fetched, it just has nothing in it. */
    @Test
    public void anEmptyKeyListMeansThereIsNoKey() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());
        connection.setCanGetKeys(true);
        final ResultSet keys = mock(ResultSet.class);
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(keys.next()).thenReturn(false);
        when(keys.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);

        connection.setKeys(keys);

        assertNull(connection.getKey());
        assertEquals(0, connection.getKeys().length);
        // an empty key list is still a list, not a missing one
        assertEquals(0, connection.getKeys(String.class).size());
    }

    @Test
    public void aKeyThatCannotBeConvertedIsReported() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());
        connection.setCanGetKeys(true);
        final ResultSet keys = mock(ResultSet.class);
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(keys.next()).thenReturn(true, false);
        when(keys.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        // BooleanConverter refuses a type it does not know, which is a ConverterException the key accessors wrap
        when(keys.getObject(1)).thenReturn(new Object());
        connection.setKeys(keys);

        assertThrows(Sql2oException.class, () -> connection.getKey(Boolean.class));
        assertThrows(Sql2oException.class, () -> connection.getKeys(Boolean.class));
    }

    @Test
    public void aNamedColumnQueryOnAClosedConnectionAsksTheSourceForANewOne() throws Exception {
        final java.sql.Connection jdbc = quietJdbcConnection();
        when(jdbc.isClosed()).thenReturn(true, false);
        final AtomicInteger acquisitions = new AtomicInteger();
        final ConnectionSource source = () -> {
            acquisitions.incrementAndGet();
            return jdbc;
        };
        final Connection connection = new Connection(sql2o(), source, false);

        connection.createQuery("insert into t values (?)", "id");

        assertEquals(2, acquisitions.get());
    }

    /** Rollback on close needs to know the auto commit mode, and that is only ever a warning. */
    @Test
    public void aConnectionThatCannotSayWhetherItIsAutoCommitIsOnlyWarnedAbout() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        // the mode is read once when the connection is taken and again when it is closed
        when(jdbc.getAutoCommit()).thenReturn(true).thenThrow(new SQLException("no idea"));
        final Connection connection = connectionOver(jdbc);

        connection.close();

        verify(jdbc).close();
    }

    /** The auto commit mode the connection came with is restored before it goes back to a pool. */
    @Test
    public void aConnectionThatCannotBeRestoredIsOnlyWarnedAbout() throws Exception {
        final java.sql.Connection jdbc = mock(java.sql.Connection.class);
        when(jdbc.isClosed()).thenReturn(false);
        when(jdbc.getAutoCommit()).thenReturn(true);
        doThrow(new SQLException("cannot restore")).when(jdbc).setAutoCommit(true);
        final Connection connection = connectionOver(jdbc);

        connection.close();

        verify(jdbc).close();
    }

    @Test
    public void theFirstGeneratedKeyIsTheOneThatIsHandedOut() throws Exception {
        final Connection connection = connectionOver(quietJdbcConnection());
        connection.setCanGetKeys(true);
        final ResultSet keys = mock(ResultSet.class);
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(keys.next()).thenReturn(true, true, false);
        when(keys.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(keys.getObject(1)).thenReturn(17L, 18L);

        connection.setKeys(keys);

        assertEquals(17L, connection.getKey());
        assertEquals(2, connection.getKeys().length);
    }
}