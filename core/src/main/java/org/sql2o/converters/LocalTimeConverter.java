package org.sql2o.converters;

import java.time.LocalTime;

public class LocalTimeConverter extends ConverterBase<LocalTime> {

    @Override
    public LocalTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof java.sql.Time) {
            return ((java.sql.Time) val).toLocalTime();
        }
        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toLocalDateTime().toLocalTime();
        }
        if (val instanceof String) {
            try {
                return LocalTime.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Can't convert String with value '" + val + "' to LocalTime", e);
            }
        }
        throw new ConverterException("Can't convert type " + val.getClass().getName() + " to LocalTime");
    }
}
