package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two calls generated code makes back into this extension, and the wording of their failures.
 *
 * <p>Conversion stays in the {@link Converter} api on purpose, so this is the one place where a converter failure is
 * worded — and the wording has to be the reflective path's, property for property, because a caller should not be able
 * to tell which path mapped its row by the message it got.
 */
public class ReaderSupportTest {

    /** A converter that converts one fixed value and refuses everything else the way a converter is meant to. */
    private static Converter<String> upperCasing() {
        return new Converter<>() {
            @Override
            public String convert(Object value) throws ConverterException {
                if (value == null) {
                    return null;
                }
                if (value instanceof String text) {
                    return text.toUpperCase();
                }
                throw new ConverterException("not a string: " + value);
            }

            @Override
            public Object toDatabaseParam(String value) {
                return value;
            }
        };
    }

    @Test
    public void aConvertedValuePassesThrough() {
        assertEquals("A ROW", ReaderSupport.convertProperty(upperCasing(), "a row", "whatever"));
    }

    @Test
    public void aNullPassesThroughWhenTheConverterTakesIt() {
        assertNull(ReaderSupport.convertProperty(upperCasing(), null, "whatever"));
    }

    @Test
    public void aConverterFailureOnAPropertyNamesTheValueAndTheProperty() {
        final Sql2oException thrown = assertThrows(Sql2oException.class,
                () -> ReaderSupport.convertProperty(upperCasing(), 7, "field text of type einiges"));

        assertTrue(thrown.getMessage().contains("value of type java.lang.Integer"),
                "the message names the type the value turned out to be: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("field text of type einiges"), thrown.getMessage());
    }

    /**
     * A converter failing on a null. The reflective path writes {@code value.getClass().getName()} into its own
     * message and so throws a {@link NullPointerException} out of the catch block; a message saying {@code null} is
     * reproduced here instead, deliberately rather than identically.
     */
    @Test
    public void aConverterFailureOnANullSaysNull() {
        final Converter<String> refusingNull = new Converter<>() {
            @Override
            public String convert(Object value) throws ConverterException {
                throw new ConverterException("no nulls");
            }

            @Override
            public Object toDatabaseParam(String value) {
                return value;
            }
        };

        final Sql2oException thrown = assertThrows(Sql2oException.class,
                () -> ReaderSupport.convertProperty(refusingNull, null, "field text of type einiges"));

        assertTrue(thrown.getMessage().contains("value of type null to"),
                "a null is named rather than thrown over: " + thrown.getMessage());
    }

    @Test
    public void aConverterFailureOnARecordComponentNamesTheColumn() {
        final Sql2oException thrown = assertThrows(Sql2oException.class,
                () -> ReaderSupport.convertRecordComponent(upperCasing(), 7, "amount", "java.math.BigDecimal"));

        assertEquals("Error trying to convert column amount to type java.math.BigDecimal", thrown.getMessage());
    }

    @Test
    public void aConvertedRecordComponentPassesThrough() {
        assertEquals("A ROW", ReaderSupport.convertRecordComponent(upperCasing(), "a row", "text", "java.lang.String"));
    }

    /** The settings helper both factory tests reach for, kept honest here rather than copied. */
    static Settings settings(boolean throwOnMappingError) {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), throwOnMappingError);
    }

    static Map<String, String> noMappings() {
        return Map.of();
    }
}
