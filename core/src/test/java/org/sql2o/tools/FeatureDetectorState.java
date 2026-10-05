package org.sql2o.tools;

import java.lang.reflect.Field;

/**
 * Reads and writes the static answer cache inside {@link FeatureDetector}, so tests can control which branch
 * they exercise instead of inheriting whatever an earlier test happened to look up.
 */
final class FeatureDetectorState {

    private FeatureDetectorState() {}

    static void setCached(String fieldName, Boolean value) throws Exception {
        final Field field = FeatureDetector.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    static void forget() throws Exception {
        setCached("jodaTimeAvailable", null);
        setCached("slf4jAvailable", null);
        setCached("oracleAvailable", null);
        FeatureDetector.setCacheUnderscoreToCamelcaseEnabled(true);
    }
}