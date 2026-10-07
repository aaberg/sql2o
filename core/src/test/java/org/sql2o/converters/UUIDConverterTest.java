package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UUIDConverterTest {

    private final UUIDConverter converter = new UUIDConverter();

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aUuidIsReturnedAsItIs() throws ConverterException {
        final UUID uuid = UUID.randomUUID();

        assertSame(uuid, converter.convert(uuid));
    }

    @Test
    public void theTextualFormIsParsed() throws ConverterException {
        final UUID uuid = UUID.randomUUID();

        assertEquals(uuid, converter.convert(uuid.toString()));
    }

    @Test
    public void somethingElseIsRefused() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> converter.convert(42L));

        assertEquals("Cannot convert type class java.lang.Long class java.util.UUID", ex.getMessage());
    }

    @Test
    public void textThatIsNotAUuidIsRefusedByTheParser() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("not a uuid"));
    }

    @Test
    public void toDatabaseParamHandsTheValueBack() {
        final UUID uuid = UUID.randomUUID();

        assertSame(uuid, converter.toDatabaseParam(uuid));
    }
}