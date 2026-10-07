package org.sql2o.converters;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;

public class LocalTimeConverter extends ConverterBase<LocalTime> {

    @Override
    public LocalTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof java.sql.Time) {
            // java.sql.Time keeps the milliseconds in its epoch value but toLocalTime() drops them, so the value is
            // read from the epoch instead of losing the fraction of a second on the way in.
            return LocalTime.ofInstant(Instant.ofEpochMilli(((java.sql.Time) val).getTime()), ZoneId.systemDefault());
        }
        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toLocalDateTime().toLocalTime();
        }
        // A driver that keeps the offset of a time with a zone hands over an OffsetTime, and asking for a plain time
        // means asking for the wall clock part of it.
        if (val instanceof OffsetTime) {
            return ((OffsetTime) val).toLocalTime();
        }
        if (val instanceof LocalTime) {
            return (LocalTime) val;
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
