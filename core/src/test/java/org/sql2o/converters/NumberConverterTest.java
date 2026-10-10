package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link NumberConverter} and the converters built on it.
 *
 * <p>The interesting behaviour is in the base class: a null or an empty string becomes zero for a primitive target
 * and null for a wrapper, and anything that is neither a Number nor a String is refused.
 */
public class NumberConverterTest {

    @Test
    public void nullBecomesZeroForAPrimitiveTarget() {
        assertEquals(0, new IntegerConverter(true).convert(null));
        assertEquals(0L, new LongConverter(true).convert(null));
        assertEquals(0.0d, new DoubleConverter(true).convert(null));
        assertEquals(0.0f, new FloatConverter(true).convert(null));
        assertEquals((short) 0, new ShortConverter(true).convert(null));
        assertEquals((byte) 0, new ByteConverter(true).convert(null));
    }

    @Test
    public void nullBecomesNullForAWrapperTarget() {
        assertNull(new IntegerConverter(false).convert(null));
        assertNull(new LongConverter(false).convert(null));
        assertNull(new DoubleConverter(false).convert(null));
        assertNull(new FloatConverter(false).convert(null));
        assertNull(new ShortConverter(false).convert(null));
        assertNull(new ByteConverter(false).convert(null));
        assertNull(new BigDecimalConverter().convert(null));
    }

    @Test
    public void anEmptyOrBlankStringIsTreatedLikeNull() {
        assertEquals(0, new IntegerConverter(true).convert(""));
        assertEquals(0, new IntegerConverter(true).convert("   "));
        assertNull(new LongConverter(false).convert(""));
        assertNull(new DoubleConverter(false).convert("  "));
        assertNull(new FloatConverter(false).convert(""));
        assertNull(new ShortConverter(false).convert(" "));
        assertNull(new ByteConverter(false).convert(""));
        assertNull(new BigDecimalConverter().convert("  "));
    }

    @Test
    public void aNumberOfAnotherTypeIsConverted() {
        assertEquals(1, new IntegerConverter(false).convert(1L));
        assertEquals(1, new IntegerConverter(false).convert(1.9d));
        assertEquals(2L, new LongConverter(false).convert((short) 2));
        assertEquals(3.0d, new DoubleConverter(false).convert(3));
        assertEquals(4.0f, new FloatConverter(false).convert(4L));
        assertEquals((short) 5, new ShortConverter(false).convert(5.7d));
        assertEquals((byte) 6, new ByteConverter(false).convert(6L));
        assertEquals(new BigDecimal("7.5"), new BigDecimalConverter().convert(7.5d));
    }

    @Test
    public void aStringIsTrimmedAndParsed() {
        assertEquals(1, new IntegerConverter(false).convert(" 1 "));
        assertEquals(2L, new LongConverter(false).convert("2"));
        assertEquals(3.5d, new DoubleConverter(false).convert("3.5"));
        assertEquals(4.5f, new FloatConverter(false).convert("4.5"));
        assertEquals((short) 5, new ShortConverter(false).convert("5"));
        assertEquals((byte) 6, new ByteConverter(false).convert("6"));
        assertEquals(new BigDecimal("7.50"), new BigDecimalConverter().convert(" 7.50 "));
    }

    @Test
    public void aValueOfTheTargetTypeIsReturnedAsItIs() {
        final BigDecimal decimal = new BigDecimal("8.25");

        assertSame(decimal, new BigDecimalConverter().convert(decimal));
    }

    @Test
    public void somethingThatIsNotANumberOrAStringIsRefused() {
        final var ex = assertThrows(IllegalArgumentException.class,
                () -> new IntegerConverter(false).convert(new Object()));

        assertEquals("Cannot convert type class java.lang.Object to class java.lang.Integer", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new LongConverter(false).convert(new Object()));
        assertThrows(IllegalArgumentException.class, () -> new DoubleConverter(false).convert(new Object()));
        assertThrows(IllegalArgumentException.class, () -> new FloatConverter(false).convert(new Object()));
        assertThrows(IllegalArgumentException.class, () -> new ShortConverter(false).convert(new Object()));
        assertThrows(IllegalArgumentException.class, () -> new ByteConverter(false).convert(new Object()));
        assertThrows(IllegalArgumentException.class, () -> new BigDecimalConverter().convert(new Object()));
    }

    @Test
    public void aStringThatIsNotANumberIsRefusedByTheParser() {
        assertThrows(NumberFormatException.class, () -> new IntegerConverter(false).convert("not a number"));
    }
}