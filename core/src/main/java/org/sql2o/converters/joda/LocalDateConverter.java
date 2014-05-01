package org.sql2o.converters.joda;

import org.joda.time.LocalDate;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

/**
 * Created by lars on 01.05.14.
 */
public class LocalDateConverter implements Converter<LocalDate> {
    @Override
    public LocalDate convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        try {
            // Joda has it's own pluggable converters infrastructure
            // it will throw IllegalArgumentException if can't convert
            // look @ org.joda.time.convert.ConverterManager
            return new LocalDate(val);
        } catch (IllegalArgumentException ex) {
            throw new ConverterException("Don't know how to convert from type '" + val.getClass().getName() + "' to type '" + LocalDate.class.getName() + "'", ex);
        }
    }

    @Override
    public Object toDatabaseParam(LocalDate val) {
        return new java.sql.Date(val.toDateTimeAtStartOfDay().getMillis());
    }
}
