package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.sql2o.quirks.NoQuirks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings on the builder, which are the five settings of core plus the fallback.
 *
 * <p>The builder is a drop-in for the one of core, so its defaults are core's: case-insensitive, no name derivation,
 * unmapped columns ignored, and the fallback on. Every setter here only records a value; the factory is built by
 * {@code newFactory}, which is covered wherever a query runs.
 */
public class BytecodeResultSetHandlerFactoryBuilderTest {

    @Test
    public void theDefaultsAreTheDefaultsOfCore() {
        final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();

        assertTrue(builder.isFallbackAllowed(), "the fallback is on unless turned off");
        assertFalse(builder.isCaseSensitive());
        assertFalse(builder.isAutoDeriveColumnNames());
        assertFalse(builder.isThrowOnMappingError());
        assertTrue(builder.getColumnMappings().isEmpty());
    }

    @Test
    public void everySettingIsRecorded() {
        final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
        final NoQuirks quirks = new NoQuirks();

        builder.setFallbackAllowed(false);
        builder.setCaseSensitive(true);
        builder.setAutoDeriveColumnNames(true);
        builder.throwOnMappingError(true);
        builder.setColumnMappings(Map.of("a", "b"));
        builder.setQuirks(quirks);

        assertFalse(builder.isFallbackAllowed());
        assertTrue(builder.isCaseSensitive());
        assertTrue(builder.isAutoDeriveColumnNames());
        assertTrue(builder.isThrowOnMappingError());
        assertEquals(Map.of("a", "b"), builder.getColumnMappings());
        assertTrue(builder.getQuirks() == quirks);
    }

    /** Setting the mappings twice replaces them, and a null clears them rather than failing. */
    @Test
    public void theMappingsAreReplacedAndANullClearsThem() {
        final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();

        builder.setColumnMappings(Map.of("a", "b"));
        builder.setColumnMappings(Map.of("c", "d"));
        assertEquals(Map.of("c", "d"), builder.getColumnMappings());

        builder.setColumnMappings(null);
        assertTrue(builder.getColumnMappings().isEmpty());
    }

    /** A plan whose columns and descriptions disagree is refused at construction rather than misread later. */
    @Test
    public void aPlanWithMismatchedColumnsAndDescriptionsIsRefused() {
        final RowReader reader = (resultSet, quirks, converters) -> null;

        assertThrows(IllegalArgumentException.class,
                () -> new RowPlan(reader, new Class<?>[]{String.class}, new String[2]));
    }

    /** What a plan says about itself, for a log line rather than for mapping. */
    @Test
    public void aPlanNamesItsReader() {
        final RowReader reader = (resultSet, quirks, converters) -> null;
        final RowPlan plan = new RowPlan(reader, new Class<?>[]{String.class}, new String[]{"text"});

        assertTrue(plan.toString().contains(reader.getClass().getName()), plan.toString());
    }
}
