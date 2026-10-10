package org.sql2o.quirks;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link QuirksDetector}, which picks the quirks implementation from the services on the classpath.
 *
 * <p>The test classpath registers H2QuirksProvider, which claims anything starting with "jdbc:h2" or with the class
 * name "org.h2". Everything else has to end up on {@link NoQuirks}.
 */
public class QuirksDetectorTest {

    /**
 * Stands in for the shape the dollar sign handling is about: a class in another package that extends a driver class.
 * What the class does is irrelevant here, only where it was declared matters.
 */
private static final class NestedInAnotherPackage extends org.h2.Driver {
    }

    @Test
    public void aProviderThatClaimsTheUrlIsUsed() {
        assertInstanceOf(H2Quirks.class, QuirksDetector.forURL("jdbc:h2:mem:test"));
    }

    @Test
    public void aUrlNoProviderClaimsFallsBackToNoQuirks() {
        assertInstanceOf(NoQuirks.class, QuirksDetector.forURL("jdbc:unknown:whatever"));
    }

    @Test
    public void aTopLevelClassIsMatchedByItsOwnName() {
        assertInstanceOf(H2Quirks.class, QuirksDetector.forObject(new JdbcDataSource()));
    }

    /**
     * The name of a nested class carries a dollar sign and would never match, so the enclosing class is asked
     * instead.
     */
    @Test
    public void aNestedClassIsMatchedByItsEnclosingClass() {
        assertInstanceOf(H2Quirks.class, QuirksDetector.forObject(new NestedInAnotherPackage()));
    }

    @Test
    public void aClassNoProviderClaimsFallsBackToNoQuirks() {
        assertInstanceOf(NoQuirks.class, QuirksDetector.forObject("a string is not a driver"));
    }

    @Test
    public void theProvidersAreFoundThroughTheServiceInterface() {
        assertTrue(QuirksDetector.providers.iterator().hasNext());
    }
}