package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DerbyQuirksProvider}, which is how {@link QuirksDetector} finds the derby implementation when
 * nobody passes quirks explicitly.
 */
public class DerbyQuirksProviderTest {

    private final DerbyQuirksProvider provider = new DerbyQuirksProvider();

    @Test
    public void itProvidesDerbyQuirks() {
        assertInstanceOf(DerbyQuirks.class, provider.provide());
    }

    @Test
    public void itClaimsTheDerbyUrls() {
        assertTrue(provider.isUsableForUrl("jdbc:derby:memory:db;create=true"));
        assertTrue(provider.isUsableForUrl("jdbc:derby:/var/lib/derby/db"));
        assertTrue(provider.isUsableForUrl("jdbc:derby://localhost:1527/db"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:hsqldb:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(provider.isUsableForUrl("jdbc:oracle:thin:@localhost:1521:XE"));
        assertFalse(provider.isUsableForUrl("jdbc:db2://localhost:50000/testdb"));
        assertFalse(provider.isUsableForUrl("derby:memory:db"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:derbyx:memory:db"));
        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test;driver=jdbc:derby:memory:db"));
    }

    /**
     * The embedded driver and the client driver, which is the one a caller reaches for when the database is on another
     * host. Both speak to the same database and both need the quirks, so both are claimed.
     */
    @Test
    public void itClaimsTheDerbyDriverClasses() {
        assertTrue(provider.isUsableForClass("org.apache.derby.jdbc.EmbeddedDriver"));
        assertTrue(provider.isUsableForClass("org.apache.derby.jdbc.ClientDriver"));
        assertTrue(provider.isUsableForClass("org.apache.derby.client.ClientAutoloadedDriver"));

        assertFalse(provider.isUsableForClass("org.apache.derby.catalog.Catalog"));
        assertFalse(provider.isUsableForClass("org.apache.derbyx.Fake"));
        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
    }

    /**
     * The provider is only of any use if the services file is in place, since this is the way quirks are picked up when
     * a Sql2o instance is built from a jdbc url alone.
     */
    @Test
    public void theDetectorPicksItUpFromTheServicesFile() {
        assertInstanceOf(DerbyQuirks.class, QuirksDetector.forURL("jdbc:derby:memory:db;create=true"));
    }
}