package org.sql2o.converters;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringConverterTest {

    private StringConverter converter;

    @Before
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
}