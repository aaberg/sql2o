package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassUtilsTest {

    @Test
    public void aClassOnTheClasspathIsFound() {
        assertTrue(ClassUtils.isPresent("java.lang.String"));
        assertTrue(ClassUtils.isPresent("org.sql2o.tools.ClassUtils"));
    }

    @Test
    public void aClassThatIsNotThereIsReportedAsAbsent() {
        assertFalse(ClassUtils.isPresent("no.such.Class"));
        assertFalse(ClassUtils.isPresent(""));
    }

    /**
     * Catching Throwable rather than ClassNotFoundException is deliberate: a class that is present but whose
     * static initialiser throws must not take the caller down with it either.
     */
    @Test
    public void aClassThatFailsToInitialiseIsReportedAsAbsent() {
        assertFalse(ClassUtils.isPresent("org.sql2o.tools.FailingInitialiser"));
    }

    /** A null name ends up in the same catch, reported as absent rather than as a NullPointerException. */
    @Test
    public void aNullNameIsReportedAsAbsent() {
        assertFalse(ClassUtils.isPresent(null));
    }
}