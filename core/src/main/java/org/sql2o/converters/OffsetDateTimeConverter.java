package org.sql2o.converters;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeConverter extends ConverterBase<OffsetDateTime>{
    @Override
    public OffsetDateTime convert(Object val) throws ConverterException {
        if (val instanceof OffsetDateTime) {
            return (OffsetDateTime) val;
        }

        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toInstant().atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
        }

        if (val instanceof Long) {
            final var instant = Instant.ofEpochMilli((Long)val);
            return instant.atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
        }

        if (val instanceof String) {
            try {
                return OffsetDateTime.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Cannot convert String with value '" + val+ "' to java.time.OffsetDateTime", e);
            }
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.time.OffsetDateTime");
    }
}
