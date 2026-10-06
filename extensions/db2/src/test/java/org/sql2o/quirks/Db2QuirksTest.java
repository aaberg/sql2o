package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.DefaultConverter;

import java.lang.reflect.Proxy;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link Db2Quirks}, which carries no behaviour of its own beyond the two constructors. What is pinned
 * here is that it carries none: the date and time handling of a driver lives in the converters of core, and the column
 * naming is the one every other driver does, so that a query written with an alias maps to the property the alias names.
 */
public class Db2QuirksTest {

    @Test
    public void theColumnIsNamedByItsAliasAndNotByItsUnderlyingName() throws SQLException {
        assertEquals("MY_ID", new Db2Quirks().getColumnName(meta(), 1));
    }

    /** The same stub read through the default quirks has to give the same answer, since db2 does not deviate. */
    @Test
    public void db2AgreesWithTheDefaultAboutColumnNames() throws SQLException {
        ResultSetMetaData meta = meta();

        assertEquals(new NoQuirks().getColumnName(meta, 1), new Db2Quirks().getColumnName(meta, 1));
    }

    @Test
    public void aLocalConverterIsUsed() {
        final Converter own = new DefaultConverter();
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(String.class, own);

        assertSame(own, new Db2Quirks(converters).converterOf(String.class));
    }

    /** The inherited behaviour has to keep working through this subclass as well. */
    @Test
    public void theDefaultsOfNoQuirksStillApply() throws SQLException {
        final Db2Quirks quirks = new Db2Quirks();

        assertEquals("MY_ID", quirks.getColumnName(meta(), 1));
        assertEquals(org.sql2o.converters.Convert.getConverterIfExists(String.class), quirks.converterOf(String.class));
    }

    /**
     * Metadata that answers differently depending on whether the label or the name is asked for, which is what makes
     * the difference visible: the label is the alias the query gave, the name is the column underneath it.
     *
     * <p>A proxy keeps this to a few lines, where implementing the interface would run to two hundred.
     */
    private static ResultSetMetaData meta() {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                Db2QuirksTest.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getColumnLabel":
                            return "MY_ID";
                        case "getColumnName":
                            return "ID";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}