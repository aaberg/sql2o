package org.sql2o.converters;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;

/**
 * Reads a {@link OffsetTime}, which is what a database column of a time with a zone comes back as. Without a converter
 * for it, a driver that preserves the offset hands the mapper a value nothing knows how to place.
 */
public class OffsetTimeConverter extends ConverterBase<OffsetTime> {
    @Override
    public OffsetTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof OffsetTime) {
            return (OffsetTime) val;
        }

        // A zoneless time has no offset of its own, so the offset the jvm has right now is as good a guess as any.
        if (val instanceof LocalTime) {
            return OffsetTime.of((LocalTime) val, OffsetTime.now(ZoneId.systemDefault()).getOffset());
        }

        // java.sql.Time and java.sql.Date inherit toInstant from java.util.Date and throw for it, on the grounds that
        // the epoch they carry means nothing. A time does have a wall clock, so it can be read, while a date has no
        // time of day to read at all.
        if (val instanceof java.sql.Time) {
            return OffsetTime.of(((java.sql.Time) val).toLocalTime(), OffsetTime.now(ZoneId.systemDefault()).getOffset());
        }

        if (val instanceof java.sql.Date) {
            throw new ConverterException("Cannot convert " + val.getClass() + " to java.time.OffsetTime: "
                    + "a date carries no time of day");
        }

        if (val instanceof java.util.Date) {
            return OffsetTime.ofInstant(((java.util.Date) val).toInstant(), ZoneId.systemDefault());
        }

        if (val instanceof Long) {
            return OffsetTime.ofInstant(Instant.ofEpochMilli((Long) val), ZoneId.systemDefault());
        }

        if (val instanceof String) {
            try {
                return OffsetTime.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Cannot convert String with value '" + val + "' to java.time.OffsetTime", e);
            }
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.time.OffsetTime");
    }
}