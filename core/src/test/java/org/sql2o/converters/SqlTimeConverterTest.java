package org.sql2o.converters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SqlTimeConverterTest {

    @Test
    public void convert_null_returnsNull() throws ConverterException {
        final var converter = new SqlTimeConverter();
        assertNull(converter.convert(null));
    }

    @Test
    public void convert_sqlTime_returnsSqlTime() throws ConverterException {
        final var converter = new SqlTimeConverter();
        final var input = java.sql.Time.valueOf("12:34:56");
        assertEquals(input, converter.convert(input));
    }
}
