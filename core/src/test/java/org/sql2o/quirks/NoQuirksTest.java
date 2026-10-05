package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.converters.DefaultConverter;
import org.sql2o.converters.Convert;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NoQuirks}, the behaviour every driver inherits unless it says otherwise.
 *
 * <p>The point of interest is the boxed parameter overloads: a null must be sent as a typed SQL NULL rather than as
 * a plain setObject, because a driver cannot always infer the type from null.
 */
public class NoQuirksTest {

    private final NoQuirks quirks = new NoQuirks();

    private static PreparedStatement statement() throws Exception {
        return mock(PreparedStatement.class);
    }

    /** A type nothing anywhere has a converter for, so only what this test registers can answer for it. */
    private static final class Custom {
    }

    private static Converter converterThatReturns(Object result) {
        return new DefaultConverter() {
            @Override
            public Object convert(Object val) {
                return result;
            }
        };
    }

    @Test
    public void aLocalConverterIsPreferred() {
        final Converter own = converterThatReturns("local");
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(Custom.class, own);

        assertSame(own, new NoQuirks(converters).converterOf(Custom.class));
    }

    /**
     * The map is copied in the constructor, so a caller that keeps hold of it and changes it afterwards cannot
     * alter the behaviour of quirks that are already in use.
     */
    @Test
    public void theConverterMapIsCopiedSoThatLaterChangesDoNotLeakIn() {
        final Converter before = converterThatReturns("before");
        final Converter after = converterThatReturns("after");
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(Custom.class, before);
        final NoQuirks withCopy = new NoQuirks(converters);

        converters.put(Custom.class, after);

        assertSame(before, withCopy.converterOf(Custom.class));
    }

    @Test
    public void aClassWithNoLocalConverterFallsBackToTheGlobalRegistry() {
        assertSame(Convert.getConverterIfExists(String.class), quirks.converterOf(String.class));
    }

    @Test
    public void aClassNobodyHasACConverterForGetsTheDefault() {
        final Converter converter = quirks.converterOf(Object.class);

        assertNotNull(converter);
        assertEquals(DefaultConverter.class, converter.getClass());
    }

    @Test
    public void theColumnNameComesFromTheLabel() throws Exception {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnLabel(2)).thenReturn("MY_COLUMN");

        assertEquals("MY_COLUMN", quirks.getColumnName(meta, 2));
    }

    @Test
    public void generatedKeysAreExpectedByDefault() {
        assertTrue(quirks.returnGeneratedKeysByDefault());
    }

    @Test
    public void theParameterParsingStrategyIsTheSameEveryTime() {
        assertNotNull(quirks.getSqlParameterParsingStrategy());
        assertSame(quirks.getSqlParameterParsingStrategy(), quirks.getSqlParameterParsingStrategy());
    }

    @Test
    public void aColumnIsReadWithGetObject() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(3)).thenReturn("value");

        assertEquals("value", quirks.getRSVal(rs, 3));
    }

    @Test
    public void aStatementIsClosed() throws Exception {
        final Statement statement = mock(Statement.class);

        quirks.closeStatement(statement);

        verify(statement).close();
    }

    @Test
    public void anObjectParameterGoesStraightToSetObject() throws Exception {
        final PreparedStatement statement = statement();
        final Object value = new Object();

        quirks.setParameter(statement, 1, value);

        verify(statement).setObject(1, value);
    }

    @Test
    public void aStreamParameterBecomesABinaryStream() throws Exception {
        final PreparedStatement statement = statement();
        final ByteArrayInputStream value = new ByteArrayInputStream(new byte[] {1, 2});

        quirks.setParameter(statement, 1, value);

        verify(statement).setBinaryStream(1, value);
    }

    @Test
    public void primitiveParametersKeepTheirType() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, 7);
        verify(statement).setInt(1, 7);

        quirks.setParameter(statement, 2, 8L);
        verify(statement).setLong(2, 8L);

        quirks.setParameter(statement, 3, true);
        verify(statement).setBoolean(3, true);
    }

    @Test
    public void aNullIntegerBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (Integer) null);

        verify(statement).setNull(1, Types.INTEGER);
    }

    @Test
    public void aPresentIntegerIsSetAsAnInt() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, Integer.valueOf(9));

        verify(statement).setInt(1, 9);
    }

    @Test
    public void aNullLongBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (Long) null);

        verify(statement).setNull(1, Types.BIGINT);
    }

    @Test
    public void aPresentLongIsSetAsALong() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, Long.valueOf(9L));

        verify(statement).setLong(1, 9L);
    }

    @Test
    public void aNullStringBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (String) null);

        verify(statement).setNull(1, Types.VARCHAR);
    }

    @Test
    public void aPresentStringIsSetAsText() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, "text");

        verify(statement).setString(1, "text");
    }

    @Test
    public void aNullTimestampBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (Timestamp) null);

        verify(statement).setNull(1, Types.TIMESTAMP);
    }

    @Test
    public void aPresentTimestampIsSetAsATimestamp() throws Exception {
        final PreparedStatement statement = statement();
        final Timestamp value = new Timestamp(1L);

        quirks.setParameter(statement, 1, value);

        verify(statement).setTimestamp(1, value);
    }

    @Test
    public void aNullTimeBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (Time) null);

        verify(statement).setNull(1, Types.TIME);
    }

    @Test
    public void aPresentTimeIsSetAsATime() throws Exception {
        final PreparedStatement statement = statement();
        final Time value = new Time(1L);

        quirks.setParameter(statement, 1, value);

        verify(statement).setTime(1, value);
    }

    @Test
    public void aNullBooleanBecomesATypedNull() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, (Boolean) null);

        verify(statement).setNull(1, Types.BOOLEAN);
    }

    @Test
    public void aPresentBooleanIsSetAsABoolean() throws Exception {
        final PreparedStatement statement = statement();

        quirks.setParameter(statement, 1, Boolean.TRUE);

        verify(statement).setBoolean(1, true);
    }

    @Test
    public void aUuidGoesThroughSetObject() throws Exception {
        final PreparedStatement statement = statement();
        final UUID value = UUID.randomUUID();

        quirks.setParameter(statement, 1, value);

        verify(statement).setObject(1, value);
    }

    @Test
    public void theDefaultConverterIsOnlyUsedAsALastResort() throws ConverterException {
        // Object.class has no converter anywhere, which is exactly the case DefaultConverter exists for
        assertEquals("value", quirks.converterOf(Object.class).convert("value"));
    }
}