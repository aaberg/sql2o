package org.sql2o.converters;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * Used by sql2o to convert a value from the database into a {@link DateTime} instance.
 */
public class JodaTimeConverter implements Converter<DateTime> {
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
}
