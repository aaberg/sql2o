package org.sql2o.converters;

import java.sql.Time;
import java.time.LocalTime;

/**
 * A {@link LocalTime} stored as a {@link Time}, for a driver that refuses the object.
 *
 * <p>The same question as {@link OffsetTimeToTimeConverter}, asked of a time with no zone on it: only the databases whose
 * driver refuses the object should hand this out, and they say so by registering it in their own quirks. Derby is one of
 * them, measured: {@code setObject} with a {@code LocalTime} fails with {@code SQLDataException: An attempt was made to
 * get a data value of type 'TIME' from a data value of type 'java.time.LocalTime'}.
 *
 * <p>What is kept is the time of day, and what is lost is the milliseconds of it, since a {@link Time} has no fraction of
 * a second to hold. Note that the conversion goes through {@link Time#valueOf(LocalTime)} rather than a
 * {@code Time.from}: that method takes an {@link java.time.Instant}, and a wall clock is the thing being stored here.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose; see
 * {@link InstantToTimestampConverter} for why.
 */
public class LocalTimeToSqlTimeConverter extends LocalTimeConverter {

    @Override
    public Object toDatabaseParam(LocalTime val) {
        return val == null ? null : Time.valueOf(val.withNano(0));
    }
}