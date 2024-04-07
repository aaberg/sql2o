package org.sql2o.converters;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LocalDateTimeConverter extends ConverterBase<LocalDateTime> {
    @Override
    public LocalDateTime convert(Object val) throws ConverterException {
        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toLocalDateTime();
        }
        if (val instanceof Long) {
            return Instant.ofEpochMilli((Long)val).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
        if (val instanceof String) {
            try {
                return LocalDateTime.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Can't convert String with value '" + val + "' to LocalDateTime", e);
            }
        }
        throw new ConverterException("Can't convert type " + val.getClass().getName() + " to LocalDateTime");
    }
}
