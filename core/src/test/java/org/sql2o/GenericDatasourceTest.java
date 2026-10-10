package org.sql2o;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link GenericDatasource}, the {@link javax.sql.DataSource} sql2o builds when it is handed a url, a user
 * and a password.
 *
 * <p>The login timeout and the log writer are process wide through {@link java.sql.DriverManager}, so they are read
 * back and restored around the assertions that touch them.
 */
public class GenericDatasourceTest {

    /**
     * H2 creates an in-memory database on the first connection and drops it with the last one, so every test needs
     * its own name to stay independent. DB_CLOSE_DELAY keeps it alive between the connections of one test.
     */
    private static String url(String name) {
        return "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
    }

    @Test
    public void aUrlWithoutTheJdbcPrefixGetsOne() {
        assertEquals("jdbc:h2:mem:x", new GenericDatasource("h2:mem:x", "u", "p").getUrl());
    }

    @Test
    public void aUrlThatAlreadyHasThePrefixIsLeftAlone() {
        assertEquals(url("x"), new GenericDatasource(url("x"), "u", "p").getUrl());
        assertEquals(url("y"), new GenericDatasource(url("y"), new Properties()).getUrl());
    }

    @Test
    public void thePropertiesConstructorAlsoAddsTheJdbcPrefix() {
        final GenericDatasource dataSource = new GenericDatasource("h2:mem:prefix", new Properties());

        assertEquals("jdbc:h2:mem:prefix", dataSource.getUrl());
    }

    @Test
    public void theUserAndThePasswordAreKept() {
        final GenericDatasource dataSource = new GenericDatasource(url("creds"), "scott", "tiger");

        assertEquals("scott", dataSource.getUser());
        assertEquals("tiger", dataSource.getPassword());
    }

    @Test
    public void aMissingUserOrPasswordStaysMissing() {
        final GenericDatasource dataSource = new GenericDatasource(url("nothing"), null, null);

        assertNull(dataSource.getUser());
        assertNull(dataSource.getPassword());
    }

    @Test
    public void propertiesAreUsedAsGiven() {
        final Properties properties = new Properties();
        properties.setProperty("user", "properties user");
        properties.setProperty("password", "properties password");

        final GenericDatasource dataSource = new GenericDatasource(url("props"), properties);

        assertEquals("properties user", dataSource.getUser());
        assertEquals("properties password", dataSource.getPassword());
    }

    @Test
    public void aConnectionIsOpenedFromTheUrlAndTheProperties() throws Exception {
        try (Connection con = new GenericDatasource(url("open_one"), "sa", "").getConnection()) {
            assertNotNull(con);
        }
    }

    @Test
    public void aConnectionCanBeOpenedWithOtherCredentials() throws Exception {
        try (Connection con = new GenericDatasource(url("open_two"), "sa", "").getConnection("sa", "")) {
            assertNotNull(con);
        }
    }

    @Test
    public void credentialsGivenToTheCallWinOverTheStoredOnes() throws Exception {
        final GenericDatasource dataSource = new GenericDatasource(url("override"), "sa", "");

        try (Connection con = dataSource.getConnection("sa", "")) {
            assertNotNull(con);
        }

        // the stored credentials are untouched by the ones handed to the call
        assertEquals("sa", dataSource.getUser());
    }

    @Test
    public void badCredentialsAreReported() throws Exception {
        final GenericDatasource dataSource = new GenericDatasource(url("bad_creds"), "sa", "");
        dataSource.getConnection().close();

        assertThrows(SQLException.class, () -> dataSource.getConnection("wrong", "wrong"));
    }

    @Test
    public void theLogWriterIsTheOneOfTheDriverManager() throws Exception {
        final PrintWriter original = java.sql.DriverManager.getLogWriter();
        try {
            final StringWriter sink = new StringWriter();
            final GenericDatasource dataSource = new GenericDatasource(url("globals"), "u", "p");

            dataSource.setLogWriter(new PrintWriter(sink));

            assertEquals(java.sql.DriverManager.getLogWriter(), dataSource.getLogWriter());
        } finally {
            java.sql.DriverManager.setLogWriter(original);
        }
    }

    @Test
    public void theLoginTimeoutIsTheOneOfTheDriverManager() throws Exception {
        final int original = java.sql.DriverManager.getLoginTimeout();
        try {
            final GenericDatasource dataSource = new GenericDatasource(url("globals"), "u", "p");

            dataSource.setLoginTimeout(7);

            assertEquals(7, dataSource.getLoginTimeout());
            assertEquals(7, java.sql.DriverManager.getLoginTimeout());
        } finally {
            java.sql.DriverManager.setLoginTimeout(original);
        }
    }

    @Test
    public void theLoggingAndUnwrappingApisAreNotSupported() throws Exception {
        final GenericDatasource dataSource = new GenericDatasource(url("globals"), "u", "p");

        assertThrows(SQLFeatureNotSupportedException.class, dataSource::getParentLogger);
        assertThrows(SQLFeatureNotSupportedException.class, () -> dataSource.unwrap(GenericDatasource.class));
        assertFalse(dataSource.isWrapperFor(GenericDatasource.class));
    }
}