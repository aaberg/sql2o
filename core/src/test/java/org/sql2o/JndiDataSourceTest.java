package org.sql2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link JndiDataSource}, which resolves a data source through JNDI.
 *
 * <p>A test JNDI provider is installed for the duration of each test, since the class asks
 * {@code new InitialContext()} for one.
 */
public class JndiDataSourceTest {

    private String previousFactory;

    @BeforeEach
    public void installTheTestProvider() {
        previousFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, JndiTestContextFactory.class.getName());
        JndiTestContextFactory.reset();
    }

    @AfterEach
    public void putTheProviderBack() {
        JndiTestContextFactory.reset();
        if (previousFactory == null) {
            System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
        } else {
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, previousFactory);
        }
    }

    @Test
    public void theDataSourceBehindTheNameIsHandedOut() {
        final DataSource dataSource = new JdbcDataSource();
        JndiTestContextFactory.lookupResult = dataSource;

        assertSame(dataSource, JndiDataSource.getJndiDatasource("java:comp/env/jdbc/test"));
    }

    @Test
    public void theContextIsClosedAfterTheLookup() {
        JndiTestContextFactory.lookupResult = new JdbcDataSource();

        JndiDataSource.getJndiDatasource("java:comp/env/jdbc/test");

        assertEquals(1, JndiTestContextFactory.closes.get());
    }

    @Test
    public void aNameThatCannotBeResolvedIsReportedAsARuntimeException() {
        JndiTestContextFactory.lookupFailure = new NameNotFoundException("no such name");

        final RuntimeException ex =
                assertThrows(RuntimeException.class, () -> JndiDataSource.getJndiDatasource("nowhere"));

        assertEquals(NameNotFoundException.class, ex.getCause().getClass());
        assertEquals(1, JndiTestContextFactory.closes.get(), "the context is closed even when the lookup failed");
    }

    /** A context that cannot be closed is only worth a warning: the data source has already been handed out. */
    @Test
    public void aContextThatFailsToCloseDoesNotFailTheLookup() {
        final DataSource dataSource = new JdbcDataSource();
        JndiTestContextFactory.lookupResult = dataSource;
        JndiTestContextFactory.closeFails = true;

        assertSame(dataSource, JndiDataSource.getJndiDatasource("java:comp/env/jdbc/test"));
    }

    /** No context means nothing to close, which the finally block has to cope with. */
    @Test
    public void aProviderThatCannotGiveAContextIsReported() {
        JndiTestContextFactory.factoryFails = true;

        final RuntimeException ex =
                assertThrows(RuntimeException.class, () -> JndiDataSource.getJndiDatasource("nowhere"));

        assertEquals(NamingException.class, ex.getCause().getClass());
        assertEquals(0, JndiTestContextFactory.closes.get());
    }

    /** The public way in: a Sql2o built from a JNDI name. */
    @Test
    public void aSql2oCanBeBuiltFromAJndiName() throws Exception {
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jndi_data_source_test");
        JndiTestContextFactory.lookupResult = dataSource;

        final Sql2o sql2o = new Sql2o("java:comp/env/jdbc/test");

        try (var con = sql2o.open()) {
            assertNotNull(con);
        }
    }
}