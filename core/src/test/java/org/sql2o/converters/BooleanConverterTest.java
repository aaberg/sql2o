package org.sql2o.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BooleanConverterTest {

    private final BooleanConverter converter = new BooleanConverter();

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aBooleanIsPassedThrough() throws ConverterException {
        assertTrue(converter.convert(true));
        assertFalse(converter.convert(false));
    }

    /**
     * Numbers go through intValue(), so the value is truncated before being compared to zero: 0.5 is false and 1.5
     * is true.
     */
    @Test
    public void aNumberIsTrueUnlessItTruncatesToZero() throws ConverterException {
        assertTrue(converter.convert(1));
        assertTrue(converter.convert(-1));
        assertTrue(converter.convert(2L));
        assertTrue(converter.convert(1.5d));
        assertTrue(converter.convert((short) 7));

        assertFalse(converter.convert(0));
        assertFalse(converter.convert(0L));
        assertFalse(converter.convert(0.0d));
        assertFalse(converter.convert(0.5d));
    }

    /** Only the upper case letters mean yes for a character, unlike the strings below. */
    @ParameterizedTest
    @ValueSource(chars = {'Y', 'T', 'J'})
    public void theCharactersThatMeanYesAreTrue(char val) throws ConverterException {
        assertTrue(converter.convert(val));
    }

    @ParameterizedTest
    @ValueSource(chars = {'y', 't', 'j', 'N', 'n', 'F', '0'})
    public void everyOtherCharacterIsFalse(char val) throws ConverterException {
        assertFalse(converter.convert(val));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Y", "yes", "TRUE", "t", "J", "  y  "})
    public void theWordsThatMeanYesAreTrueAndAreTrimmed(String val) throws ConverterException {
        assertTrue(converter.convert(val));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N", "no", "false", "0", "", "   ", "anything else"})
    public void everyOtherStringIsFalse(String val) throws ConverterException {
        assertFalse(converter.convert(val));
    }

    @Test
    public void somethingThatIsNotAYesOrNoIsRefused() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> converter.convert(new Object()));

        assertEquals("Don't know how to convert type java.lang.Object to java.lang.Boolean", ex.getMessage());
    }

    @Test
    public void toDatabaseParamHandsTheValueBack() {
        assertEquals(Boolean.TRUE, converter.toDatabaseParam(true));
    }
}