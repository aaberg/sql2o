package org.sql2o.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}