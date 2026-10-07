package org.sql2o.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * A {@link LocalDateTime} stored as a {@link Timestamp}, for a driver that refuses the object.
 *
 * <p>The same question as {@link InstantToTimestampConverter}, asked of a date and a time with no zone on them: only the
 * databases whose driver refuses the object should hand this out, and they say so by registering it in their own quirks.
 * Derby is one of them, measured: {@code setObject} with a {@code LocalDateTime} fails with
 * {@code SQLDataException: An attempt was made to get a data value of type 'TIMESTAMP' from a data value of type
 * 'java.time.LocalDateTime'}.
 *
 * <p>What is kept is the date and the time of day, both of which a {@link Timestamp} holds exactly; the zone that is not
 * on a {@link LocalDateTime} in the first place is not lost here.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose; see
 * {@link InstantToTimestampConverter} for why.
 */
public class LocalDateTimeToTimestampConverter extends LocalDateTimeConverter {

    @Override
    public Object toDatabaseParam(LocalDateTime val) {
        return val == null ? null : Timestamp.valueOf(val);
    }
}