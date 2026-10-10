package org.sql2o.converters;

import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import oracle.sql.TIMESTAMPTZ;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
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

    private static final Timestamp JANUARY_FIRST = Timestamp.valueOf("2020-01-01 00:00:00");

    private final OracleDateConverter converter = new OracleDateConverter();

    @Test
    public void itReadsAnOracleTimestamp() throws ConverterException {
        Timestamp jdkTimestamp = Timestamp.valueOf("2020-01-01 12:34:56");

        assertEquals(jdkTimestamp, converter.convert(new TIMESTAMP(jdkTimestamp)));
    }

    @Test
    public void itReadsAnOracleDateAndTimestamp() throws ConverterException, SQLException {
        assertEquals(JANUARY_FIRST, converter.convert(new TIMESTAMP(JANUARY_FIRST)));
        assertEquals(new DATE(JANUARY_FIRST).timestampValue(), converter.convert(new DATE(JANUARY_FIRST)));
    }

    /**
     * The offset travels inside the value, so a TIMESTAMPTZ can be read wherever it came from. Only the instant is
     * asserted, since a java.util.Date cannot remember which offset it was read at and rendering it in the zone of the
     * jvm is the whole point of keeping the instant rather than a wall clock.
     */
    @Test
    public void itReadsAZoneAwareTimestampByItsOffset() throws ConverterException, SQLException {
        OffsetDateTime inUtc = OffsetDateTime.parse("2020-06-01T12:30:15+02:00");

        Date converted = converter.convert(new TIMESTAMPTZ(inUtc));

        assertEquals(inUtc.toInstant(), converted.toInstant());
    }

    @Test
    public void itReportsAFailingTimestampAsAConverterException() {
        ConverterException thrown =
                assertThrows(ConverterException.class, () -> converter.convert(new ExplodingTimestamp()));

        assertEquals("Error trying to convert " + ExplodingTimestamp.class.getName() + " to java.util.Date",
                thrown.getMessage());
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

    @Test
    public void itReportsAFailingZoneAwareTimestampAsAConverterException() {
        ConverterException thrown =
                assertThrows(ConverterException.class, () -> converter.convert(new ExplodingZoneAwareTimestamp()));

        assertEquals("Error trying to convert " + ExplodingZoneAwareTimestamp.class.getName() + " to java.util.Date",
                thrown.getMessage());
        assertEquals("boom", thrown.getCause().getMessage());
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

    /** Same trick for the zone aware type, which is read through a different accessor. */
    private static class ExplodingZoneAwareTimestamp extends TIMESTAMPTZ {

        ExplodingZoneAwareTimestamp() throws SQLException {
            super(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        }

        @Override
        public OffsetDateTime offsetDateTimeValue() throws SQLException {
            throw new SQLException("boom");
        }
    }
}
