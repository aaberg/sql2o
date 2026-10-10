package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HsqlQuirksProvider}, which is how {@link QuirksDetector} finds the hsqldb implementation when
 * nobody passes quirks explicitly.
 */
public class HsqlQuirksProviderTest {

    private final HsqlQuirksProvider provider = new HsqlQuirksProvider();

    @Test
    public void itProvidesHsqlQuirks() {
        assertInstanceOf(HsqlQuirks.class, provider.provide());
    }

    @Test
    public void itClaimsTheHsqlUrls() {
        assertTrue(provider.isUsableForUrl("jdbc:hsqldb:mem:test"));
        assertTrue(provider.isUsableForUrl("jdbc:hsqldb:hsql://localhost:9000/db"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(provider.isUsableForUrl("jdbc:oracle:thin:@localhost:1521:XE"));
        assertFalse(provider.isUsableForUrl("jdbc:db2://localhost:50000/testdb"));
        assertFalse(provider.isUsableForUrl("hsqldb:mem:test"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:hsqldbx:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test;driver=jdbc:hsqldb:mem:test"));
    }

    @Test
    public void itClaimsTheHsqlDriverClasses() {
        assertTrue(provider.isUsableForClass("org.hsqldb.jdbcDriver"));
        assertTrue(provider.isUsableForClass("org.hsqldb.jdbc.JDBCConnection"));

        assertFalse(provider.isUsableForClass("org.hsqldb.persist.HsqlDatabaseManager"));
        assertFalse(provider.isUsableForClass("org.hsqldbx.Fake"));
        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
    }

    /**
     * The provider is only of any use if the services file is in place, since this is the way quirks are picked up when
     * a Sql2o instance is built from a jdbc url alone.
     */
    @Test
    public void theDetectorPicksItUpFromTheServicesFile() {
        assertInstanceOf(HsqlQuirks.class, QuirksDetector.forURL("jdbc:hsqldb:mem:test"));
    }
}