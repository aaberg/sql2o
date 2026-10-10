package org.sql2o.tools;

/**
 * Only here so that {@link ClassUtilsTest} can check what happens to a class that is on the classpath but whose
 * static initialiser blows up. Nothing in the library is allowed to reference it.
 */
class FailingInitialiser {

    static {
        if (System.nanoTime() > 0) {
            throw new IllegalStateException("this class refuses to initialise");
        }
    }
}