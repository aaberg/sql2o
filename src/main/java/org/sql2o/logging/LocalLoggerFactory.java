package org.sql2o.logging;

import org.slf4j.LoggerFactory;

/**
 * Created by lars on 2/9/14.
 */
public class LocalLoggerFactory {

    public static Logger getLogger(Class clazz){
        if (isSlfAvailable()){
            return new Slf4jLogger(LoggerFactory.getLogger(clazz));
        }

        return new SysOutLogger(clazz);
    }

    private static boolean isSlfAvailable() {
        try {
            Class.forName("org.slf4j.Logger");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
