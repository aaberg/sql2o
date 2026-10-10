package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link RecordBuilder}, the mapping path for Java records.
 *
 * <p>Records bypass PojoMetadata entirely, so nothing else in the suite reaches the two failure paths here.
 */
public class RecordBuilderTest {

    public record Point(int x, int y) {}

    /** Always fails, to reach the conversion error path. */
    public static class FailingConverter implements Converter<Object> {

        @Override
        public Object convert(Object val) throws ConverterException {
            throw new ConverterException("cannot convert " + val);
        }

        @Override
        public Object toDatabaseParam(Object val) {
            return val;
        }
    }

    private static Settings plainSettings() {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), true);
    }

    private static Settings failingSettings() {
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(int.class, new FailingConverter());
        return new Settings(new NamingConvention(false, false), new NoQuirks(converters), true);
    }

    private static RecordBuilder<Point> builder(Settings settings) {
        return new RecordBuilder<>(Point.class, settings);
    }

    @Test
    public void everyComponentIsFilledFromItsColumn() throws ReflectiveOperationException {
        final var builder = builder(plainSettings());

        builder.withValue("x", 1);
        builder.withValue("y", 2);

        assertEquals(new Point(1, 2), builder.build());
    }

/**
     * Documents current behaviour: RecordBuilder hands every component to the canonical constructor, so a
     * component no column was mapped for is passed as null. For a primitive component the JDK refuses that with
     * an IllegalArgumentException. The message is reflection internals and differs between JDK releases — it names
     * ValueConversions on newer ones and is null on 17 — so only the type is pinned here, never the wording.
     */
    @Test
    public void aComponentLeftOutFails() {
        final var builder = builder(plainSettings());

        builder.withValue("x", 1);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void aColumnWithoutAComponentIsRejected() {
        final var builder = builder(plainSettings());

        final var ex = assertThrows(IllegalArgumentException.class, () -> builder.withValue("z", 1));

        assertEquals("No such field in record: z", ex.getMessage());
    }

    @Test
    public void aFailingConverterIsReported() {
        final var builder = builder(failingSettings());

        final var ex = assertThrows(Sql2oException.class, () -> builder.withValue("x", 1));

        assertEquals("Error trying to convert column x to type int", ex.getMessage());
    }
}