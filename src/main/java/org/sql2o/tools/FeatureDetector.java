package org.sql2o.tools;

/**
 * Detects whether optional sql2o features are available.
 *
 * @author Alden Quimby
 */
public final class FeatureDetector {

    private FeatureDetector()
    {}

    private static Boolean jodaTimeAvailable;
    private static Boolean slf4jAvailable;

    /**
     * @return {@code true} if Joda-Time is available, {@code false} otherwise.
     */
    public static boolean isJodaTimeAvailable() {
        if (jodaTimeAvailable == null) {
            jodaTimeAvailable = ClassUtils.isPresent("org.joda.time.DateTime");
        }
        return jodaTimeAvailable;
    }

    /**
     * @return {@code true} if Slf4j is available, {@code false} otherwise.
     */
    public static boolean isSlf4jAvailable() {
        if (slf4jAvailable == null) {
            slf4jAvailable = ClassUtils.isPresent("org.slf4j.Logger");
        }
        return slf4jAvailable;
    }
}