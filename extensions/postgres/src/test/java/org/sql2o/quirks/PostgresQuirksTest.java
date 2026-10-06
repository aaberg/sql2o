package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.DefaultConverter;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PostgresQuirks}.
 *
 * <p>Apart from generated keys it changes one thing about reading, and that is covered by
 * {@code PostgresTemporalTypesTest} against a real database. What is left here is the spelling of the column type name
 * that the driver never produces, which no database can be made to produce here.
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

    /**
     * The driver reports a time with a zone as {@code timetz}, but the sql standard spelling is
     * {@code time with time zone} and there is no reason to turn a column down over it. Both spellings have to be
     * recognised, which is why the second one is kept even though the integration test never sees it.
     */
    @Test
    public void theStandardSpellingOfTheTypeNameIsRecognisedToo() throws SQLException {
        assertSame(TYPED_READ_RETURNED, readColumnNamed("time with time zone"));
    }

    /** And a time without a zone must be left exactly as the driver returned it. */
    @Test
    public void aTimeWithoutAZoneIsLeftAlone() throws SQLException {
        assertSame(DRIVER_VALUE, readColumnNamed("time"));
    }

    /** A column that is not a time at all must not even have its metadata looked at. */
    @Test
    public void aColumnThatIsNotATimeIsReturnedUnchanged() throws SQLException {
        assertSame(DRIVER_VALUE, readColumnNamed("int4"));
    }

    private static final OffsetTime TYPED_READ_RETURNED =
            OffsetTime.of(12, 34, 56, 789_000_000, ZoneOffset.ofHours(2));

    private static final java.sql.Time DRIVER_VALUE = java.sql.Time.valueOf("12:34:56");

    private Object readColumnNamed(String typeName) throws SQLException {
        return new PostgresQuirks().getRSVal(stubResultSet(typeName), 1);
    }

    /**
     * A result set that reports the given type name, hands over a plain time from the untyped read, and answers the
     * typed read for an OffsetTime with a value of its own. Getting that value back is what shows the quirks asked for
     * it rather than settling for what the untyped read returned. A proxy keeps this to a few lines, where
     * implementing the interface would run to two hundred.
     */
    private static ResultSet stubResultSet(String typeName) {
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                PostgresQuirksTest.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnTypeName".equals(method.getName())) {
                        return typeName;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        return (ResultSet) Proxy.newProxyInstance(
                PostgresQuirksTest.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getObject":
                            if (method.getParameterCount() == 2) {
                                if (OffsetTime.class.equals(args[1])) {
                                    return TYPED_READ_RETURNED;
                                }
                                throw new UnsupportedOperationException("no conversion to " + args[1]);
                            }
                            return DRIVER_VALUE;
                        case "getMetaData":
                            return metaData;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}