package org.sql2o.quirks;

import org.junit.Test;

import static org.junit.Assert.*;

public class QuirksDetectorTest {

    @Test
    public void willDefaultToSameInstanceOfNoQuirksIfNothingIsFoundForObj(){
        Object jdbcObject = new Object();
        assertSame(QuirksDetector.forObject(jdbcObject), QuirksDetector.forObject(jdbcObject));
    }

    @Test
    public void willDefaultToSameInstanceOfNoQuirksIfNothingIsFoundForUrl(){
        assertSame(QuirksDetector.forURL(""), QuirksDetector.forURL(""));
    }
}
