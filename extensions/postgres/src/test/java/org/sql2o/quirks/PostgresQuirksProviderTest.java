package org.sql2o.quirks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PostgresQuirksProvider}, which is how {@link QuirksDetector} finds the postgres
 * implementation when nobody passes quirks explicitly.
 */
public class PostgresQuirksProviderTest {

    private final PostgresQuirksProvider provider = new PostgresQuirksProvider();

    @Test
    public void itProvidesPostgresQuirks() {
        assertInstanceOf(PostgresQuirks.class, provider.provide());
    }

    @Test
    public void itClaimsPostgresUrlsOnly() {
        assertTrue(provider.isUsableForUrl("jdbc:postgresql://localhost:5432/postgres"));

        assertFalse(provider.isUsableForUrl("jdbc:h2:mem:test"));
        assertFalse(provider.isUsableForUrl("jdbc:oracle:thin:@localhost"));
        assertFalse(provider.isUsableForUrl("postgresql://localhost"));
    }

    @Test
    public void itClaimsPostgresClassesOnly() {
        assertTrue(provider.isUsableForClass("org.postgresql.jdbc.PgConnection"));
        assertTrue(provider.isUsableForClass("org.postgresql.Driver"));

        assertFalse(provider.isUsableForClass("com.example.SomethingElse"));
        assertFalse(provider.isUsableForClass("org.postgresqlish.Fake"));
    }

    /** The detector asks every provider in turn, so a provider that claims everything would win outright. */
    @Test
    public void itDoesNotClaimUrlsThatOnlyLookSimilar() {
        assertFalse(provider.isUsableForUrl("jdbc:postgresqlx://localhost"));
    }
}