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
        
        if (Date.class.isAssignableFrom( val.getClass() )){
            return (Date)val;
        }
        
        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Date");
    }

    public Object toDatabaseParam(Date val) {
        return new Timestamp(val.getTime());
    }
}
