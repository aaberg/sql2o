package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AbstractDateConverter}, through {@link DateConverter} and the anonymous converters that
 * {@link Convert} registers for {@link java.sql.Date} and {@link Timestamp}.
 */
public class AbstractDateConverterTest {

    private static final long MILLIS = 1_600_000_000_000L;

    private final DateConverter converter = new DateConverter();

    @Test
    public void nullBecomesNull() throws ConverterException {
        assertNull(converter.convert(null));
    }

    @Test
    public void aDateOfTheTargetTypeIsReturnedAsItIs() throws ConverterException {
        final Date date = new Date(MILLIS);

        assertSame(date, converter.convert(date));
    }

    /**
     * java.sql.Timestamp is a java.util.Date, so the target type check already accepts it and no conversion
     * happens.
     */
    @Test
    public void aSubclassOfTheTargetTypeIsReturnedAsItIs() throws ConverterException {
        final Timestamp timestamp = new Timestamp(MILLIS);

        assertSame(timestamp, converter.convert(timestamp));
    }

/**
 * Every java.util.Date is an instance of Date, so this converter never reaches the "some other kind of date"
 * branch: that is only reachable for a narrower target such as java.sql.Date, covered below.
 */
@Test
public void anyDateIsReturnedAsItIs() throws ConverterException {
        final Date plain = new Date(MILLIS);
        final java.sql.Date sqlDate = new java.sql.Date(MILLIS);

        assertSame(plain, converter.convert(plain));
        assertSame(sqlDate, converter.convert(sqlDate));
    }

    @Test
    public void aNumberIsTreatedAsMilliseconds() throws ConverterException {
        assertEquals(MILLIS, converter.convert(MILLIS).getTime());
        assertEquals(MILLIS, converter.convert(Long.valueOf(MILLIS)).getTime());
    }

    @Test
    public void somethingThatIsNotADateOrANumberIsRefused() {
        final ConverterException ex =
                assertThrows(ConverterException.class, () -> converter.convert("2020-01-01"));

        assertEquals("Cannot convert type class java.lang.String to java.util.Date", ex.getMessage());
    }

    @Test
    public void theSqlDateConverterRebuildsFromMilliseconds() throws ConverterException {
        final Converter<java.sql.Date> sqlDateConverter = Convert.getConverter(java.sql.Date.class);

        assertEquals(new java.sql.Date(MILLIS), sqlDateConverter.convert(new Date(MILLIS)));
        assertEquals(new java.sql.Date(MILLIS), sqlDateConverter.convert(MILLIS));
        assertNull(sqlDateConverter.convert(null));
    }

    @Test
    public void theTimestampConverterRebuildsFromMilliseconds() throws ConverterException {
        final Converter<Timestamp> timestampConverter = Convert.getConverter(Timestamp.class);

        assertEquals(new Timestamp(MILLIS), timestampConverter.convert(new Date(MILLIS)));
        assertEquals(new Timestamp(MILLIS), timestampConverter.convert(MILLIS));
        assertNull(timestampConverter.convert(null));
    }

    @Test
    public void aNullDateIsNotWrittenToTheDatabase() throws ConverterException {
        assertNull(converter.toDatabaseParam(null));
    }

    @Test
    public void aTimestampIsAlreadyInTheRightShapeForTheDatabase() throws ConverterException {
        final Timestamp timestamp = new Timestamp(MILLIS);

        assertSame(timestamp, converter.toDatabaseParam(timestamp));
    }

    @Test
    public void anyOtherDateIsWrittenAsATimestamp() throws ConverterException {
        final Object param = converter.toDatabaseParam(new Date(MILLIS));

        assertTrue(param instanceof Timestamp);
        assertEquals(MILLIS, ((Timestamp) param).getTime());
    }

    @Test
    public void theSqlTimeConverterUsesItsOwnShape() throws ConverterException {
        final Converter<Time> timeConverter = Convert.getConverter(Time.class);

        assertEquals(new Time(MILLIS), timeConverter.convert(new Date(MILLIS)));
    }
}