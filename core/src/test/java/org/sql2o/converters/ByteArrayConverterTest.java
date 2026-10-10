package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteArrayConverterTest {

    private static final byte[] CONTENT = "some bytes".getBytes(StandardCharsets.UTF_8);

    private final ByteArrayConverter converter = new ByteArrayConverter();

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aByteArrayIsReturnedAsItIs() throws ConverterException {
        assertSame(CONTENT, converter.convert(CONTENT));
    }

    @Test
    public void aBlobIsReadThroughAndThenReleased() throws Exception {
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(CONTENT));

        assertArrayEquals(CONTENT, converter.convert(blob));
        verify(blob).free();
    }

    @Test
    public void theStreamOfABlobIsClosedToo() throws Exception {
        final InputStream stream = mock(InputStream.class);
        when(stream.read(any(byte[].class))).thenThrow(new IOException("nope"));
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenReturn(stream);

        assertThrows(ConverterException.class, () -> converter.convert(blob));

        verify(stream).close();
        verify(blob).free();
    }

    @Test
    public void aBlobThatCannotBeOpenedIsReported() throws Exception {
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenThrow(new SQLException("no stream"));

        final ConverterException ex = assertThrows(ConverterException.class, () -> converter.convert(blob));

        assertEquals("Error converting Blob to byte[]", ex.getMessage());
        verify(blob).free();
    }

    @Test
    public void aStreamThatFailsWhileReadingIsReported() throws Exception {
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }
        });

        final ConverterException ex = assertThrows(ConverterException.class, () -> converter.convert(blob));

        assertEquals("Error converting Blob to byte[]", ex.getMessage());
    }

    /**
     * Closing and freeing are best effort: the value has already been read, so a failure there must not lose it.
     */
    @Test
    public void failuresWhileClosingAndFreeingAreSwallowed() throws Exception {
        final InputStream stream = mock(InputStream.class);
        when(stream.read(any(byte[].class))).thenThrow(new IOException("nope"));
        doThrow(new IllegalStateException("cannot close")).when(stream).close();
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenReturn(stream);
        doThrow(new IllegalStateException("cannot free")).when(blob).free();

        assertThrows(ConverterException.class, () -> converter.convert(blob));
    }

    @Test
    public void somethingThatIsNotBytesOrABlobIsRefused() {
        final var ex = assertThrows(RuntimeException.class, () -> converter.convert("not bytes"));

        assertEquals("could not convert java.lang.String to byte[]", ex.getMessage());
    }
}