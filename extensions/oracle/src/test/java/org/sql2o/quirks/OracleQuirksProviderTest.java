package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OracleQuirksProvider}, which is how {@link QuirksDetector} finds the oracle implementation when
 * nobody passes quirks explicitly. The provider is listed in META-INF/services/org.sql2o.quirks.QuirksProvider.
 */
public class OracleQuirksProviderTest {

    private final OracleQuirksProvider provider = new OracleQuirksProvider();

    @Test
    public void itProvidesOracleQuirks() {
        assertInstanceOf(OracleQuirks.class, provider.provide());
    }

    @Test
    public void itClaimsOracleUrlsOnly() {
        assertTrue(provider.isUsableForUrl("jdbc:oracle:thin:@localhost:1521:XE"));
        assertTrue(provider.isUsableForUrl("jdbc:oracle:thin:@//localhost:1521/XE"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(provider.isUsableForUrl("oracle:thin:@localhost"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:oraclex:thin:@localhost"));
    }

    @Test
    public void itClaimsOracleDriverClassesOnly() {
        assertTrue(provider.isUsableForClass("oracle.jdbc.OracleDriver"));
        assertTrue(provider.isUsableForClass("oracle.jdbc.driver.OracleDriver"));

        assertFalse(provider.isUsableForClass("oracle.jdbcx.Fake"));
        assertFalse(provider.isUsableForClass("jdbcx.oracle.Fake"));
        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
    }

    /**
     * The provider is only useful if the services file is in place, since this is the way quirks are picked up when a
     * Sql2o instance is built from a jdbc url alone.
     */
    @Test
    public void theDetectorPicksItUpFromTheServicesFile() {
        assertInstanceOf(OracleQuirks.class, QuirksDetector.forURL("jdbc:oracle:thin:@localhost:1521:XE"));
    }
}
