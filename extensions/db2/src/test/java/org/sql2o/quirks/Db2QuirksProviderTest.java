package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Db2QuirksProvider}, which is how {@link QuirksDetector} finds the db2 implementation when nobody
 * passes quirks explicitly.
 */
public class Db2QuirksProviderTest {

    private final Db2QuirksProvider provider = new Db2QuirksProvider();

    @Test
    public void itProvidesDb2Quirks() {
        assertInstanceOf(Db2Quirks.class, provider.provide());
    }

    @Test
    public void itClaimsTheDb2Urls() {
        assertTrue(provider.isUsableForUrl("jdbc:db2://localhost:50000/testdb"));
        assertTrue(provider.isUsableForUrl("jdbc:db2j:net://host:50000/testdb"));
        assertTrue(provider.isUsableForUrl("jdbc:db2os390:..."));
        assertTrue(provider.isUsableForUrl("jdbc:db2:"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(provider.isUsableForUrl("jdbc:oracle:thin:@localhost:1521:XE"));
        assertFalse(provider.isUsableForUrl("db2://localhost:50000/testdb"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:db2x://localhost:50000/testdb"));
    }

    @Test
    public void itClaimsTheDb2DriverClasses() {
        assertTrue(provider.isUsableForClass("com.ibm.db2.jcc.DB2Driver"));
        assertTrue(provider.isUsableForClass("com.ibm.db2.jcc.DB2Connection"));

        assertFalse(provider.isUsableForClass("com.ibm.db2x.DB2"));
        assertFalse(provider.isUsableForClass("jdbx.Fake"));
        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
    }

    /**
     * The provider is only of any use if the services file is in place, since this is the way quirks are picked up when
     * a Sql2o instance is built from a jdbc url alone.
     */
    @Test
    public void theDetectorPicksItUpFromTheServicesFile() {
        assertInstanceOf(Db2Quirks.class, QuirksDetector.forURL("jdbc:db2://localhost:50000/testdb"));
    }
}