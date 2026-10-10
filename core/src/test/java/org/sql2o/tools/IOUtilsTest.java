package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IOUtilsTest {

    /** Larger than the 4k buffer, so both loops have to run more than once. */
    private static final String LARGE = "x".repeat(10000);

    @Test
    public void anEmptyStreamBecomesAnEmptyArray() throws Exception {
        assertArrayEquals(new byte[0], IOUtils.toByteArray(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void aSmallStreamIsReadInOneGo() throws Exception {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), IOUtils.toByteArray(stream("abc")));
    }

    @Test
    public void aStreamLargerThanTheBufferIsReadInSeveralGoes() throws Exception {
        assertArrayEquals(LARGE.getBytes(StandardCharsets.UTF_8), IOUtils.toByteArray(stream(LARGE)));
    }

    /**
     * InputStream may legally report zero bytes read without being at the end of the stream, so the loop has to
     * keep going instead of treating it as done.
     */
    @Test
    public void aReadThatReportsNoBytesIsNotTheEndOfTheStream() throws Exception {
        final InputStream stream = new InputStream() {

            private int state;

            @Override
            public int read() {
                return -1;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (state == 0) {
                    state = 1;
                    return 0;
                }
                if (state == 1) {
                    state = 2;
                    buffer[offset] = 'o';
                    buffer[offset + 1] = 'k';
                    return 2;
                }
                return -1;
            }
        };

        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), IOUtils.toByteArray(stream));
    }

    @Test
    public void anEmptyReaderBecomesAnEmptyString() throws Exception {
        assertEquals("", IOUtils.toString(new StringReader("")));
    }

    @Test
    public void aSmallReaderIsReadInOneGo() throws Exception {
        assertEquals("abc", IOUtils.toString(new StringReader("abc")));
    }

    @Test
    public void aReaderLargerThanTheBufferIsReadInSeveralGoes() throws Exception {
        assertEquals(LARGE, IOUtils.toString(new StringReader(LARGE)));
    }

    @Test
    public void multibyteCharactersSurviveTheReader() throws Exception {
        assertEquals("Привет", IOUtils.toString(new StringReader("Привет")));
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}