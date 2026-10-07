package org.sql2o;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NamingConventionTest {

    private static NamingConvention convention(boolean caseSensitive, boolean autoDerive) {
        return new NamingConvention(caseSensitive, autoDerive);
    }

    @Test
    public void namesAreLowercasedUnlessTheConventionIsCaseSensitive() {
        assertEquals("my_column", convention(false, false).deriveName("MY_COLUMN"));
        assertEquals("MY_COLUMN", convention(true, false).deriveName("MY_COLUMN"));
    }

    /**
 * Deriving names and matching case insensitively undo each other: the camel case is produced first and then the
 * whole name is lowercased, so the combination gives an all lower case name rather than a camel cased one.
 */
@Test
    public void snakeCaseIsTurnedIntoCamelCaseWhenAskedFor() {
        assertEquals("myColumn", convention(true, true).deriveName("MY_COLUMN"));
        assertEquals("mycolumn", convention(false, true).deriveName("MY_COLUMN"));
    }

    @Test
    public void aNameWithoutUnderscoresIsLeftAlone() {
        assertEquals("id", convention(false, true).deriveName("ID"));
        assertEquals("id", convention(false, false).deriveName("id"));
    }

    @Test
    public void twoConventionsWithTheSameFlagsAreEqual() {
        assertEquals(convention(true, false), convention(true, false));
        assertEquals(convention(true, false).hashCode(), convention(true, false).hashCode());
    }

    @Test
    public void aConventionIsEqualToItself() {
        final NamingConvention convention = convention(true, false);

        assertEquals(convention, convention);
    }

    @Test
    public void aSingleDifferingFlagMakesConventionsDifferent() {
        assertNotEquals(convention(true, false), convention(false, false));
        assertNotEquals(convention(true, true), convention(true, false));
    }

    @Test
    public void aConventionIsNotEqualToNullOrToAnotherType() {
        assertNotEquals(convention(true, false), null);
        assertNotEquals(convention(true, false), "not a convention");
    }

    /**
     * Equality is class based, so a subclass with the same flags is deliberately not equal to the plain class. That
     * stops a subclass carrying extra state from being used as a cache key in its place.
     */
    @Test
    public void aSubclassIsNotEqualToItsParentWithTheSameFlags() {
        final NamingConvention plain = convention(true, false);
        final NamingConvention subclass = new NamingConvention(true, false) {
        };

        assertNotEquals(plain, subclass);
        assertNotEquals(subclass, plain);
    }

    @Test
    public void equalConventionsHashTheSame() {
        assertTrue(convention(false, true).hashCode() == convention(false, true).hashCode());
    }
}