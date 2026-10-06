package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.DefaultConverter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PostgresQuirks}.
 *
 * <p>The only thing it changes against {@link NoQuirks} is that generated keys are not returned by default, so that
 * a caller has to ask for them; the integration tests all pass the flag explicitly and never reach this method.
 */
public class PostgresQuirksTest {

    @Test
    public void generatedKeysAreNotReturnedUnlessAskedFor() {
        assertFalse(new PostgresQuirks().returnGeneratedKeysByDefault());
    }

    @Test
    public void aLocalConverterIsUsed() {
        final Converter own = new DefaultConverter();
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(String.class, own);

        assertSame(own, new PostgresQuirks(converters).converterOf(String.class));
    }

    /** The inherited behaviour has to keep working through this subclass as well. */
    @Test
    public void theDefaultsOfNoQuirksStillApply() {
        final PostgresQuirks quirks = new PostgresQuirks();

        assertSame(org.sql2o.converters.Convert.getConverterIfExists(String.class), quirks.converterOf(String.class));
        assertTrue(quirks.getSqlParameterParsingStrategy() != null);
    }
}