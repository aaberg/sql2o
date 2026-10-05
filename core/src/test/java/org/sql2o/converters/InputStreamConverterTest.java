package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.sql2o.tools.IOUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputStreamConverterTest {

    private static final byte[] CONTENT = "some bytes".getBytes(StandardCharsets.UTF_8);

    private final InputStreamConverter converter = new InputStreamConverter();

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void bytesAreWrappedInAStream() throws Exception {
        assertArrayEquals(CONTENT, IOUtils.toByteArray(converter.convert(CONTENT)));
    }

    /**
 * A Blob that cannot be read makes the byte array converter throw, and that is the case this wrapper exists for.
 */
@Test
public void aBlobThatCannotBeReadIsReportedHere() throws Exception {
    final Blob blob = mock(Blob.class);
    when(blob.getBinaryStream()).thenThrow(new SQLException("no stream"));

    final ConverterException ex = assertThrows(ConverterException.class, () -> converter.convert(blob));

    assertEquals("Error converting Blob to InputSteam", ex.getMessage());
}

/**
 * Not everything the byte array converter rejects comes back as a ConverterException, so this wrapper does not
 * catch everything: an unsupported type escapes as the bare RuntimeException the converter threw.
 */
@Test
public void anUnsupportedTypeEscapesUnwrapped() {
    final var ex = assertThrows(RuntimeException.class, () -> converter.convert("not bytes"));

    assertEquals("could not convert java.lang.String to byte[]", ex.getMessage());
}

    @Test
    public void aBlobIsAlsoAcceptedAsInput() throws Exception {
        final Blob blob = mock(Blob.class);
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(CONTENT));

        assertArrayEquals(CONTENT, IOUtils.toByteArray(converter.convert(blob)));
    }
}