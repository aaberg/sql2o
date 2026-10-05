package org.sql2o.converters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sql2o.converters.MoneyConverterProvider.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link Convert}, the registry every query goes through.
 *
 * <p>The registry is static and there is no way to unregister anything, so each test works on a copy that is put
 * back afterwards. Without that, a converter registered here would leak into the rest of the suite.
 */
public class ConvertTest {

    private enum Colour {
        RED, GREEN
    }

    private Map<Class<?>, Converter<?>> pristineConverters;
    private EnumConverterFactory pristineEnumFactory;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void takeOverTheRegistry() throws Exception {
        pristineConverters = (Map<Class<?>, Converter<?>>) field("registeredConverters").get(null);
        pristineEnumFactory = (EnumConverterFactory) field("registeredEnumConverterFactory").get(null);

        field("registeredConverters").set(null, new HashMap<Class<?>, Converter<?>>(pristineConverters));
    }

    @AfterEach
    public void giveTheRegistryBack() throws Exception {
        field("registeredConverters").set(null, pristineConverters);
        field("registeredEnumConverterFactory").set(null, pristineEnumFactory);
    }

    private static Field field(String name) throws Exception {
        final Field field = Convert.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test
    public void theDefaultsAreRegisteredForPrimitivesAndWrappers() {
        assertNotNull(Convert.getConverterIfExists(int.class));
        assertNotNull(Convert.getConverterIfExists(Integer.class));
        assertNotNull(Convert.getConverterIfExists(boolean.class));
        assertNotNull(Convert.getConverterIfExists(Boolean.class));
        assertNotNull(Convert.getConverterIfExists(String.class));
        assertNotNull(Convert.getConverterIfExists(byte[].class));
        assertNotNull(Convert.getConverterIfExists(java.util.UUID.class));
        assertNotNull(Convert.getConverterIfExists(java.time.Instant.class));
        assertNotNull(Convert.getConverterIfExists(java.util.Date.class));
        assertNotNull(Convert.getConverterIfExists(java.sql.Date.class));
    }

    @Test
    public void aRegisteredConverterIsHandedOutAsItIs() {
        assertSame(Convert.getConverterIfExists(String.class), Convert.getConverterIfExists(String.class));
    }

    @Test
    public void aClassNobodyRegisteredHasNoConverter() {
        assertNull(Convert.getConverterIfExists(Object.class));
    }

    @Test
    public void askingForAMissingConverterSaysWhichClassItWas() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> Convert.getConverter(Object.class));

        assertEquals("No converter registered for class: java.lang.Object", ex.getMessage());
    }

    @Test
    public void anEnumFallsBackToTheEnumConverterFactory() throws ConverterException {
        final Converter<Colour> converter = Convert.getConverterIfExists(Colour.class);

        assertEquals(Colour.GREEN, converter.convert("GREEN"));
    }

    @Test
    public void aRegisteredConverterTakesPrecedenceOverTheEnumFactory() throws ConverterException {
        final Converter<Colour> own = new Converter<Colour>() {
            @Override
            public Colour convert(Object val) {
                return Colour.RED;
            }

            @Override
            public Object toDatabaseParam(Colour val) {
                return val.name();
            }
        };

        Convert.registerConverter(Colour.class, own);

        assertSame(own, Convert.getConverterIfExists(Colour.class));
        assertEquals(Colour.RED, Convert.getConverterIfExists(Colour.class).convert("GREEN"));
    }

    @Test
    public void aCustomEnumConverterFactoryIsUsedForEnums() throws ConverterException {
        final Converter<Colour> own = new Converter<Colour>() {
            @Override
            public Colour convert(Object val) {
                return Colour.GREEN;
            }

            @Override
            public Object toDatabaseParam(Colour val) {
                return val.name();
            }
        };

        Convert.registerEnumConverter(new EnumConverterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <E extends Enum> Converter<E> newConverter(Class<E> enumClass) {
                return (Converter<E>) own;
            }
        });

        assertEquals(Colour.GREEN, Convert.getConverterIfExists(Colour.class).convert("anything"));
    }

    @Test
    public void anEnumConverterFactoryIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> Convert.registerEnumConverter(null));
    }

    @Test
    public void throwIfNullOnlyComplainsAboutNull() throws ConverterException {
        final Converter<String> converter = Convert.getConverterIfExists(String.class);

        assertSame(converter, Convert.throwIfNull(String.class, converter));

        final ConverterException ex = assertThrows(ConverterException.class,
                () -> Convert.throwIfNull(Object.class, null));

        assertEquals("No converter registered for class: java.lang.Object", ex.getMessage());
    }

    /**
     * ConvertersProvider is the service interface for adding converters, so it has to be picked up from
     * META-INF/services without anyone registering it by hand.
     */
    @Test
    public void aConverterFromAServiceProviderIsRegistered() throws ConverterException {
        final Converter<Money> converter = Convert.getConverter(MoneyConverterProvider.Money.class);

        assertEquals(150L, converter.convert(150L).getCents());
        assertNull(converter.convert(null));
        assertEquals(7L, converter.convert(new Money(7L)).getCents());
        assertThrows(ConverterException.class, () -> converter.convert("not money"));
    }
}