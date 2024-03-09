package org.sql2o.converters;

public class SqlTimeConverter extends ConverterBase<java.sql.Time>{
    @Override
    public java.sql.Time convert(Object val) throws ConverterException {
        if (val instanceof java.sql.Time) {
            return (java.sql.Time) val;
        }

        if (val instanceof java.util.Date) {
            return new java.sql.Time(((java.util.Date) val).getTime());
        }

        if (val instanceof java.time.OffsetTime) {
            return java.sql.Time.valueOf(((java.time.OffsetTime) val).toLocalTime());
        }

        if (val instanceof Number) {
            return new java.sql.Time(((Number) val).longValue());
        }

        if (val instanceof String) {
            return java.sql.Time.valueOf((String) val);
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.sql.Time");
    }
}
