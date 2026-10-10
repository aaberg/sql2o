package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MysqlQuirksProvider}, which is how {@link QuirksDetector} finds the mysql implementation when
 * nobody passes quirks explicitly.
 */
public class MysqlQuirksProviderTest {

    private final MysqlQuirksProvider provider = new MysqlQuirksProvider();

    @Test
    public void itProvidesMysqlQuirks() {
        assertInstanceOf(MysqlQuirks.class, provider.provide());
    }

    @Test
    public void itClaimsTheMysqlUrls() {
        assertTrue(provider.isUsableForUrl("jdbc:mysql://localhost:13306/testdb"));
        assertTrue(provider.isUsableForUrl("jdbc:mysql://dbhost:3306/db?serverTimezone=UTC"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(provider.isUsableForUrl("jdbc:oracle:thin:@localhost:1521:XE"));
        assertFalse(provider.isUsableForUrl("jdbc:db2://localhost:50000/testdb"));
        assertFalse(provider.isUsableForUrl("mysql://localhost:13306/testdb"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:mysqlx://localhost:13306/testdb"));
        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test;driver=jdbc:mysql://localhost:13306/testdb"));
    }

    /**
     * The MariaDB driver has urls and classes of its own, which this provider leaves alone: the server the tests run
     * against answers either driver, but the quirks are found for the driver that is actually there.
     */
    @Test
    public void itDoesNotClaimTheMariadbDriver() {
        assertFalse(provider.isUsableForUrl("jdbc:mariadb://localhost:13306/testdb"));
        assertFalse(provider.isUsableForClass("org.mariadb.jdbc.Driver"));
    }

    @Test
    public void itClaimsTheMysqlDriverClasses() {
        assertTrue(provider.isUsableForClass("com.mysql.cj.jdbc.Driver"));
        assertTrue(provider.isUsableForClass("com.mysql.cj.jdbc.ConnectionImpl"));

        assertFalse(provider.isUsableForClass("com.mysql.cj.conf.PropertySet"));
        assertFalse(provider.isUsableForClass("com.mysqlx.Fake"));
        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
    }

    /**
     * The provider is only of any use if the services file is in place, since this is the way quirks are picked up when
     * a Sql2o instance is built from a jdbc url alone.
     */
    @Test
    public void theDetectorPicksItUpFromTheServicesFile() {
        assertInstanceOf(MysqlQuirks.class, QuirksDetector.forURL("jdbc:mysql://localhost:13306/testdb"));
    }
}
