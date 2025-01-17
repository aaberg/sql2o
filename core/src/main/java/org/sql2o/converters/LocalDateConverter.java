package org.sql2o.converters;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class LocalDateConverter extends ConverterBase<LocalDate> {
    @Override
    public LocalDate convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toLocalDate();
        }
        if (val instanceof Long) {
            return Instant.ofEpochMilli((Long) val).atOffset(ZoneOffset.UTC).toLocalDate();
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
