package org.sql2o.logging;

import org.slf4j.LoggerFactory;
import org.sql2o.tools.FeatureDetector;

/**
 * Created by lars on 2/9/14.
 */
public class LocalLoggerFactory {

    public static Logger getLogger(Class clazz) {
        if (FeatureDetector.isSlf4jAvailable()) {
            return new Slf4jLogger(LoggerFactory.getLogger(clazz));
        }

        return SysOutLogger.instance;
    }
}
