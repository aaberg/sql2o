package org.sql2o.converters;

import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.Assert.assertEquals;

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

}
