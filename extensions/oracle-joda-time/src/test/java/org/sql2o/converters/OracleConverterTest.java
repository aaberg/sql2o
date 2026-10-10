package org.sql2o.converters;

import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by lars on 01.05.14.
 */
public class OracleConverterTest {

    @Test
    public void testDateConversion( ) throws ConverterException {

        // assert java.sql.Date.
        long millis = LocalDate.now().toDateTimeAtStartOfDay().getMillis();
        java.sql.Date sqlDate = new java.sql.Date( millis );
        TIMESTAMP oracleTimestamp = new TIMESTAMP(sqlDate);
        Date convertedDate = Convert.getConverterIfExists(Date.class).convert(oracleTimestamp);

        assertEquals(sqlDate, convertedDate);

        // assert java.util.Date
        Date origDate = new Date();
        oracleTimestamp = new TIMESTAMP(new Timestamp(origDate.getTime()));
        convertedDate = Convert.getConverterIfExists(Date.class).convert(oracleTimestamp);

        assertEquals(origDate, convertedDate);
    }


    @Test
    public void testDateTimeConverter() throws ConverterException {
        DateTime d = DateTime.now();
        TIMESTAMP oracleTimestamp = new TIMESTAMP(new Timestamp(d.getMillis()));
        DateTime convertedDateTime = Convert.getConverterIfExists(DateTime.class).convert(oracleTimestamp);

        assertEquals(d, convertedDateTime);
    }

    @Test
    public void testLocalDateConverter() throws ConverterException {
        LocalDate lc = LocalDate.now();

        DATE oracleDate = new DATE(new java.sql.Date(lc.toDateTimeAtStartOfDay().getMillis()));

        LocalDate convertedDate = Convert.getConverterIfExists(LocalDate.class).convert(oracleDate);

        assertEquals(lc, convertedDate);
    }

    @Test
    public void testLocalTimeConverter() throws ConverterException {
        LocalTime lt = LocalTime.now();
        TIMESTAMP oracleTime = new TIMESTAMP(new Timestamp(lt.toDateTimeToday().getMillis()));
        LocalTime convertedTime = Convert.getConverterIfExists(LocalTime.class).convert(oracleTime);

        assertEquals(lt, convertedTime);
    }

    @Test
    public void testConvertersFallBackToThePlainJodaOnes() throws ConverterException {

        DateTime dateTime = DateTime.now();
        assertEquals(dateTime, Convert.getConverterIfExists(DateTime.class).convert(dateTime));

        LocalDate localDate = LocalDate.now();
        assertEquals(localDate, Convert.getConverterIfExists(LocalDate.class).convert(localDate));

        LocalTime localTime = LocalTime.now();
        assertEquals(localTime, Convert.getConverterIfExists(LocalTime.class).convert(localTime));

        assertNull(Convert.getConverterIfExists(DateTime.class).convert(null));
        assertNull(Convert.getConverterIfExists(LocalDate.class).convert(null));
        assertNull(Convert.getConverterIfExists(LocalTime.class).convert(null));
    }

    @Test
    public void testConvertersReportAFailingDatumAsAConverterException() {

        ConverterException thrown =
                assertThrows(ConverterException.class,
                        () -> Convert.getConverterIfExists(DateTime.class).convert(new ExplodingTimestamp()));
        assertEquals("Error trying to convert oracle timestamp to org.joda.time.DateTime", thrown.getMessage());
        assertEquals("boom", thrown.getCause().getMessage());

        thrown = assertThrows(ConverterException.class,
                () -> Convert.getConverterIfExists(LocalDate.class).convert(new ExplodingTimestamp()));
        assertEquals("Error trying to convert oracle date to org.joda.time.LocalDate", thrown.getMessage());
        assertEquals("boom", thrown.getCause().getMessage());

        thrown = assertThrows(ConverterException.class,
                () -> Convert.getConverterIfExists(LocalTime.class).convert(new ExplodingTimestamp()));
        assertEquals("Error trying to convert oracle time to org.joda.time.LocalTime", thrown.getMessage());
        assertEquals("boom", thrown.getCause().getMessage());
    }

    /**
     * A datum that fails on the way out is the only way to reach the catch blocks: oracle hands out timestamps that do
     * convert, so the happy paths alone would leave them dark.
     */
    private static class ExplodingTimestamp extends TIMESTAMP {

        ExplodingTimestamp() {
            super(new Timestamp(0));
        }

        @Override
        public Timestamp timestampValue() throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public java.sql.Date dateValue() throws SQLException {
            throw new SQLException("boom");
        }
    }
}
