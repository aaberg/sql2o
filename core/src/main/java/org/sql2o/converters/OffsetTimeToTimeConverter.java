package org.sql2o.converters;

import java.sql.Time;
import java.time.OffsetTime;

/**
 * An {@link OffsetTime} stored as a {@link Time}, which drops the offset and keeps the wall clock.
 *
 * <p>The same question as {@link OffsetDateTimeToTimestampConverter}, asked of a time of day: only the databases whose
 * driver refuses the object should hand this out, and they say so by registering it in their own quirks. A database with
 * a {@code time with time zone} column, such as h2 or postgres, has somewhere to keep the offset and is better off with
 * the object.
 *
 * <p>What is kept is the time of day, and what is lost is the offset, so reading back gives the offset of the jvm doing
 * the reading. Note that the conversion goes through {@link Time#valueOf(java.time.LocalTime)} rather than a
 * {@code Time.from}: that method takes an {@link java.time.Instant}, and a wall clock is the thing being stored here.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose; see
 * {@link InstantToTimestampConverter} for why.
 */
public class OffsetTimeToTimeConverter extends OffsetTimeConverter {

    @Override
    public Object toDatabaseParam(OffsetTime val) {
        return val == null ? null : Time.valueOf(val.toLocalTime());
    }
}