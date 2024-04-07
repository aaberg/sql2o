package org.sql2o.converters;

import java.time.Instant;
import java.time.LocalDate;

public class LocalDateConverter extends ConverterBase<LocalDate> {
    @Override
    public LocalDate convert(Object val) throws ConverterException {
        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toLocalDate();
        }
        if (val instanceof Number) {
            return Instant.ofEpochMilli(((Number) val).longValue()).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        if (val instanceof String) {
            try {
                return LocalDate.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Cannot convert string with value '" + val + " to java.time.LocalDate", e);
            }
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.time.LocalDate");
    }
}
