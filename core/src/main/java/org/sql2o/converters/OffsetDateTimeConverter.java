package org.sql2o.converters;

import java.time.OffsetDateTime;

public class OffsetDateTimeConverter extends ConverterBase<OffsetDateTime>{
    @Override
    public OffsetDateTime convert(Object val) throws ConverterException {
        if (val instanceof OffsetDateTime) {
            return (OffsetDateTime) val;
        }

        if (val instanceof java.util.Date) {
            return ((java.util.Date) val).toInstant().atOffset(java.time.ZoneOffset.UTC);
        }

        if (val instanceof Number) {
            return java.time.Instant.ofEpochMilli(((Number) val).longValue()).atOffset(java.time.ZoneOffset.UTC);
        }

        if (val instanceof String) {
            return OffsetDateTime.parse((String) val);
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.time.OffsetDateTime");
    }
}
