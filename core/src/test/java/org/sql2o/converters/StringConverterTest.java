package org.sql2o.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StringConverterTest {

    private StringConverter converter;

    @BeforeEach
    public void setup() {
        converter = new StringConverter();
    }

    @Test
    public void convert_shouldNotTrimWhitespace_whenGivenString() throws ConverterException {
        // Arrange
        String expected = " Hello world! ";

        // Act
        String actual = converter.convert(expected);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aClobIsReadAsTextAndThenReleased() throws Exception {
        final Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(5L);
        when(clob.getSubString(1, 5)).thenReturn("hello");

        assertEquals("hello", converter.convert(clob));
        verify(clob).free();
    }

    @Test
    public void aClobThatCannotBeReadIsReported() throws Exception {
        final Clob clob = mock(Clob.class);
        when(clob.length()).thenThrow(new SQLException("cannot measure"));

        final ConverterException ex = assertThrows(ConverterException.class, () -> converter.convert(clob));

        assertEquals("error converting clob to String", ex.getMessage());
        verify(clob).free();
    }

    @Test
    public void aClobThatCannotBeReleasedDoesNotLoseTheText() throws Exception {
        final Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(2L);
        when(clob.getSubString(1, 2)).thenReturn("hi");
        doThrow(new IllegalStateException("cannot free")).when(clob).free();

        assertEquals("hi", converter.convert(clob));
    }

    @Test
    public void aReaderIsReadToTheEndAndThenClosed() throws Exception {
        final Reader reader = mock(Reader.class);
        when(reader.read(any(char[].class))).thenReturn(-1);

        assertEquals("", converter.convert(reader));
        verify(reader).close();
    }

    @Test
    public void aRealReaderIsReadToTheEnd() throws Exception {
        assertEquals(" Hello world! ", converter.convert(new StringReader(" Hello world! ")));
    }

    @Test
    public void aReaderThatFailsIsReported() throws Exception {
        final Reader reader = mock(Reader.class);
        when(reader.read(any(char[].class))).thenThrow(new IOException("broken"));

        final ConverterException ex = assertThrows(ConverterException.class, () -> converter.convert(reader));

        assertEquals("error converting reader to String", ex.getMessage());
        verify(reader).close();
    }

    @Test
    public void aReaderThatCannotBeClosedDoesNotLoseTheText() throws Exception {
        final Reader reader = mock(Reader.class);
        when(reader.read(any(char[].class))).thenReturn(-1);
        doThrow(new IllegalStateException("cannot close")).when(reader).close();

        assertEquals("", converter.convert(reader));
    }

    @Test
    public void anythingElseFallsBackToItsOwnText() throws ConverterException {
        assertEquals("42", converter.convert(42));
        assertEquals("true", converter.convert(true));
    }

}