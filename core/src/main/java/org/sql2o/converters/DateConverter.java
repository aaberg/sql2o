package org.sql2o.converters;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Used by sql2o to convert a value from the database into a {@link Date}.
 */
public class DateConverter implements Converter<Date> {
    
    public Date convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        
        if (val instanceof Date){
            return (Date)val;
        }

        if (val instanceof Number){
            return new Date(((Number) val).longValue());
        }
        
        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Date");
    }

    public Object toDatabaseParam(Date val) {
        return new Timestamp(val.getTime());
    }
}
