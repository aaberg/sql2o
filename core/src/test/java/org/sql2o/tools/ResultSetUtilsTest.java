package org.sql2o.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResultSetUtils}. Nothing in core calls this any more, which is why it carries a TODO to
 * remove it in 2.0.
 *
 * <p>The Oracle branch cannot be covered here: it needs a value whose class name starts with
 * "oracle.sql.TIMESTAMP", and putting a stand-in for a vendor type on the core test classpath would make
 * {@link FeatureDetector#isOracleAvailable()} answer true for every core test. Testing the real thing belongs to
 * the oracle extension, which has a driver.
 */
public class ResultSetUtilsTest {

    @BeforeEach
    @AfterEach
    public void forgetTheDetectedFeatures() throws Exception {
        FeatureDetectorState.forget();
    }

    @Test
    public void aNullColumnComesBackAsNull() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        assertNull(ResultSetUtils.getRSVal(rs, 1));
        verify(rs, never()).getTimestamp(1);
    }

    @Test
    public void aValueComesBackUnchangedWhenOracleIsNotThere() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        final LocalDateTime value = LocalDateTime.now();
        when(rs.getObject(1)).thenReturn(value);

        assertSame(value, ResultSetUtils.getRSVal(rs, 1));
        verify(rs, never()).getTimestamp(1);
    }

    @Test
    public void aValueThatIsNotAnOracleTimestampIsLeftAlone() throws Exception {
        FeatureDetectorState.setCached("oracleAvailable", true);
        final ResultSet rs = mock(ResultSet.class);
        final LocalDateTime value = LocalDateTime.now();
        when(rs.getObject(1)).thenReturn(value);

        assertSame(value, ResultSetUtils.getRSVal(rs, 1));
        verify(rs, never()).getTimestamp(1);
    }
}