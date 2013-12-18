package org.sql2o.converters;

import org.joda.time.LocalTime;

import java.sql.Time;

/**
 * Created by lars on 12/18/13.
 */
public class LocalTimeConverter implements Converter<LocalTime> {
    public LocalTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }

        if (!(val instanceof Time)){
            throw new ConverterException("Don't know how to convert from type '" + val.getClass().getName() + "' to type '" + LocalTime.class.getName() + "'");
        }

        return new LocalTime(val);
    }
}
