package org.sql2o.extensions.postgres.converters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.sql2o.converters.Convert;
import org.sql2o.converters.ConverterException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JSONConverter}, which turns a json column into a gson tree and back.
 *
 * <p>It is registered through the service interface, so one test checks that the lookup by type works rather than
 * only calling the converter directly.
 */
public class JSONConverterTest {

    private final JSONConverter converter = new JSONConverter();

    private static PGobject pgobject(String value) throws SQLException {
        final PGobject pgobject = new PGobject();
        pgobject.setType("json");
        pgobject.setValue(value);
        return pgobject;
    }

    private static JsonObject json(String field, String value) {
        final JsonObject object = new JsonObject();
        object.addProperty(field, value);
        return object;
    }

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aTreeIsPassedThrough() throws ConverterException {
        final JsonElement element = json("name", "Alice");

        assertSame(element, converter.convert(element));
    }

    @Test
    public void textIsParsed() throws ConverterException {
        final JsonElement parsed = converter.convert("{\"name\":\"Alice\"}");

        assertEquals("Alice", parsed.getAsJsonObject().get("name").getAsString());
    }

    /** Postgres hands json columns back as a PGobject rather than as a string. */
    @Test
    public void aPostgresJsonObjectIsUnwrappedAndParsed() throws Exception {
        final JsonElement parsed = converter.convert(pgobject("{\"name\":\"Alice\"}"));

        assertEquals("Alice", parsed.getAsJsonObject().get("name").getAsString());
    }

    /** Anything else goes through the string converter first, which is what makes plain values work. */
    @Test
    public void anyOtherValueIsTurnedIntoTextFirst() throws ConverterException {
        assertEquals(42, converter.convert(42).getAsInt());
    }

    /**
     * Text that is not json escapes as gson's own unchecked exception, because the method declares the checked
     * ConverterException and never converts it. Same shape as the issues recorded in docs/converter-exceptions.md.
     */
    @Test
    public void malformedJsonEscapesUnwrapped() {
        assertThrows(JsonSyntaxException.class, () -> converter.convert("not json"));
    }

    @Test
    public void nullIsNotWrittenToTheDatabase() {
        assertNull(converter.toDatabaseParam(null));
    }

    @Test
    public void aTreeIsWrittenAsText() {
        assertEquals("{\"name\":\"Alice\"}", converter.toDatabaseParam(json("name", "Alice")));
    }

    /** The converter registers itself for the tree type, not for some other json type. */
    @Test
    public void fillRegistersItselfForTheTreeType() {
        final Map<Class<?>, org.sql2o.converters.Converter<?>> converters = new HashMap<>();

        converter.fill(converters);

        assertSame(converter, converters.get(JsonElement.class));
        assertEquals(1, converters.size());
    }

    /** It reaches the registry through META-INF/services, so the lookup by type has to find it. */
    @Test
    public void itIsFoundThroughTheServiceRegistry() {
        assertSame(converter.getClass(), Convert.getConverterIfExists(JsonElement.class).getClass());
    }
}