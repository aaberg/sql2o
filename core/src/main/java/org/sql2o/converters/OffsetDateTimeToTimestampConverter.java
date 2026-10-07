package org.sql2o.converters;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

/**
 * An {@link OffsetDateTime} stored as a {@link Timestamp}, which drops the offset and keeps the moment.
 *
 * <p>Unlike an {@link Instant}, a value of this type does carry an offset, so whether it can be stored as it is depends
 * entirely on the database: postgres and oracle have a column type with a zone in it and take the object, hsqldb and h2
 * take it too but have nowhere to keep the offset, and db2 has neither the type nor the patience. Only the databases
 * that need this should hand it out, and they say so by registering it in their own quirks.
 *
 * <p>What is kept is the point on the timeline, since that is what a {@link Timestamp} holds, and a column with a zone
 * in it would have normalised it to the instant anyway. What is lost is the label of the offset, and reading it back
 * therefore gives the offset of the jvm doing the reading rather than the one that was written. A caller who needs the
 * offset kept as written should store it in a column with a zone, on a database that has one.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose; see
 * {@link InstantToTimestampConverter} for why, and {@link OffsetTimeToTimeConverter} for the same question about a time
 * of day.
 */
public class OffsetDateTimeToTimestampConverter extends OffsetDateTimeConverter {

    @Override
    public Object toDatabaseParam(OffsetDateTime val) {
        return val == null ? null : Timestamp.from(val.toInstant());
    }
}