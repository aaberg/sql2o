package org.sql2o.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FeatureDetector}.
 *
 * <p>The detector answers once per JVM and remembers the answer in a static field, so these tests reset that
 * field to control which branch they exercise. joda-time and slf4j are {@code provided} dependencies of core and
 * therefore present while testing; oracle.sql is not.
 */
public class FeatureDetectorTest {

    @BeforeEach
    @AfterEach
    public void forgetEverything() throws Exception {
        FeatureDetectorState.forget();
    }

    @Test
    public void jodaTimeIsOnTheTestClasspath() {
        assertTrue(FeatureDetector.isJodaTimeAvailable());
    }

    @Test
    public void slf4jIsOnTheTestClasspath() {
        assertTrue(FeatureDetector.isSlf4jAvailable());
    }

    @Test
    public void theAnswerIsRememberedInsteadOfBeingLookedUpAgain() throws Exception {
        assertTrue(FeatureDetector.isJodaTimeAvailable());
        FeatureDetectorState.setCached("jodaTimeAvailable", false);

        assertFalse(FeatureDetector.isJodaTimeAvailable());
    }

    @Test
    public void slf4jIsRememberedTheSameWay() throws Exception {
        assertTrue(FeatureDetector.isSlf4jAvailable());
        FeatureDetectorState.setCached("slf4jAvailable", false);

        assertFalse(FeatureDetector.isSlf4jAvailable());
    }

    /**
     * Oracle is not a core dependency, so the first answer depends on the classpath, while a forced value shows
     * the second call going straight to the cache.
     */
    @Test
    public void theOracleAnswerIsLookedUpOnceAndThenRemembered() throws Exception {
        assertFalse(FeatureDetector.isOracleAvailable());
        FeatureDetectorState.setCached("oracleAvailable", true);

        assertTrue(FeatureDetector.isOracleAvailable());
    }

    @Test
    public void underscoreToCamelcaseCachingIsOnByDefaultAndCanBeTurnedOff() {
        assertTrue(FeatureDetector.isCacheUnderscoreToCamelcaseEnabled());

        FeatureDetector.setCacheUnderscoreToCamelcaseEnabled(false);
        assertFalse(FeatureDetector.isCacheUnderscoreToCamelcaseEnabled());

        FeatureDetector.setCacheUnderscoreToCamelcaseEnabled(true);
        assertTrue(FeatureDetector.isCacheUnderscoreToCamelcaseEnabled());
    }

    /** A utility class should not be instantiable. */
    @Test
    public void theDetectorCannotBeInstantiated() throws Exception {
        final Constructor<?>[] constructors = FeatureDetector.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }
}