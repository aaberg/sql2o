package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DefaultEnumConverterFactory} and the converter it hands out.
 */
public class DefaultEnumConverterFactoryTest {

    private enum Colour {
        RED, GREEN, BLUE
    }

    private final Converter<Colour> converter = new DefaultEnumConverterFactory().newConverter(Colour.class);

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aConstantIsLookedUpByName() throws ConverterException {
        assertEquals(Colour.GREEN, converter.convert("GREEN"));
    }

    @Test
    public void aNumberIsTreatedAsAnOrdinalAndIsTruncated() throws ConverterException {
        assertEquals(Colour.RED, converter.convert(0));
        assertEquals(Colour.BLUE, converter.convert(2));
        assertEquals(Colour.GREEN, converter.convert(1.9d));
    }

    @Test
    public void aNameThatIsNotAConstantIsReported() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> converter.convert("PURPLE"));

        assertEquals("Error converting value 'PURPLE' to " + Colour.class.getName(), ex.getMessage());
    }

    @Test
    public void anOrdinalOutOfRangeIsReported() {
        assertThrows(ConverterException.class, () -> converter.convert(99));
    }

    @Test
    public void somethingThatIsNeitherTextNorAnOrdinalIsReported() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> converter.convert(new Object()));

        assertEquals("Cannot convert type 'java.lang.Object' to an Enum", ex.getMessage());
    }

    @Test
    public void anEnumIsWrittenToTheDatabaseByName() {
        assertEquals("BLUE", converter.toDatabaseParam(Colour.BLUE));
    }

    @Test
    public void theConverterIsBoundToTheEnumItWasMadeFor() throws ConverterException {
        final Converter<Suit> other = new DefaultEnumConverterFactory().newConverter(Suit.class);

        assertEquals(Suit.HEARTS, Convert.getConverterIfExists(Suit.class).convert("HEARTS"));
        // and the enum converter for another type refuses the names of this one
        assertThrows(ConverterException.class, () -> other.convert("RED"));
    }

    private enum Suit {
        HEARTS, SPADES
    }
}