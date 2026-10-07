package org.sql2o.converters;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * An {@link Instant} stored as a {@link Timestamp}, which is what every database here accepts in a timestamp column.
 *
 * <p>An instant is a point on the timeline and carries no offset, so there is exactly one right way to write one down
 * and it is the same for every database. That is why this lives in core and why the reading side is inherited unchanged
 * from {@link InstantConverter}: a value that came out of a timestamp column was a {@link Timestamp} to begin with.
 *
 * <p>Left as an object instead, it reaches the driver as a {@link Instant}, and what happens then is up to the driver
 * rather than to sql2o. Measured against the databases supported here: h2 and hsqldb take one as it is, postgres refuses
 * it with {@code SQLSTATE 07006}, oracle with {@code ORA-17004}, and db2 with {@code ERRORCODE=-4461}. Asking the driver
 * for an explicit type does not help either, which is why the conversion happens here and not in the driver.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose: only a driver whose own driver refuses
 * a {@link Instant} should hand one out, and it does so by registering an instance of this class in its own quirks, where
 * {@link org.sql2o.quirks.NoQuirks#converterOf(Class)} looks before it looks at the global registry. A driver that takes
 * an {@link Instant} as it is keeps the converter of core and loses nothing.
 */
public class InstantToTimestampConverter extends InstantConverter {

    @Override
    public Object toDatabaseParam(Instant val) {
        return val == null ? null : Timestamp.from(val);
    }
}