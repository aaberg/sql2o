package org.sql2o.converters.joda;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.sql.Timestamp;

/**
 * Used by sql2o to convert a value from the database into a {@link DateTime} instance.
 */
public class DateTimeConverter implements Converter<DateTime> {

    public DateTime convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        
        try{
            return new LocalDateTime(val).toDateTime(DateTimeZone.UTC);
        }
        catch(Throwable t){
            throw new ConverterException("Error while converting type " + val.getClass().toString() + " to jodatime", t);
        }
    }

    public Object toDatabaseParam(DateTime val) {
        return new Timestamp(val.getMillis());
    }
}
