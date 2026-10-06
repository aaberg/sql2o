package org.sql2o.converters;

import oracle.sql.TIMESTAMP;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link OracleDateConverter}, the converter the oracle extension registers for {@link Date} through
 * META-INF/services/org.sql2o.converters.ConvertersProvider.
 */
public class OracleDateConverterTest {

    private final OracleDateConverter converter = new OracleDateConverter();

    @Test
    public void itReadsAnOracleTimestamp() throws ConverterException {
        Timestamp jdkTimestamp = Timestamp.valueOf("2020-01-01 12:34:56");

        assertEquals(jdkTimestamp, converter.convert(new TIMESTAMP(jdkTimestamp)));
    }

    @Test
    public void itReportsAFailingTimestampAsAConverterException() {
        ConverterException thrown =
                assertThrows(ConverterException.class, () -> converter.convert(new ExplodingTimestamp()));

        assertEquals("Error trying to convert oracle.sql.TIMESTAMP to DateTime", thrown.getMessage());
        assertEquals("boom", thrown.getCause().getMessage());
    }

    @Test
    public void itLeavesEverythingElseToThePlainDateConverter() throws ConverterException {
        assertNull(converter.convert(null));
        assertEquals(new Date(0), converter.convert(new Timestamp(0)));
        assertEquals(new Date(1234), converter.convert(1234L));

        assertThrows(ConverterException.class, () -> converter.convert("2020-01-01"));
    }

    @Test
    public void itRegistersItselfForDate() {
        Map<Class<?>, Converter<?>> converters = new HashMap<>();

        converter.fill(converters);

        assertInstanceOf(OracleDateConverter.class, converters.get(Date.class));
    }

    @Test
    public void itIsTheDateConverterTheServiceLoaderPicks() {
        assertInstanceOf(OracleDateConverter.class, Convert.getConverterIfExists(Date.class));
    }

    /**
     * A datum that fails on the way out is the only way to reach the catch block: oracle hands out timestamps that do
     * convert, so the happy path alone would leave it dark.
     */
    private static class ExplodingTimestamp extends TIMESTAMP {

        ExplodingTimestamp() {
            super(new Timestamp(0));
        }

        @Override
        public Timestamp timestampValue() throws SQLException {
            throw new SQLException("boom");
        }
    }
}
