package org.sql2o.converters;

import java.sql.Date;
import java.time.LocalDate;

/**
 * A {@link LocalDate} stored as a {@link Date}, for a driver that refuses the object.
 *
 * <p>The same question as {@link OffsetDateTimeToTimestampConverter}, asked of a date with no zone and no time of day:
 * only the databases whose driver refuses the object should hand this out, and they say so by registering it in their own
 * quirks. Derby is one of them, measured: {@code setObject} with a {@code LocalDate} fails with
 * {@code SQLDataException: An attempt was made to get a data value of type 'DATE' from a data value of type
 * 'java.time.LocalDate'}, and asking the driver for an explicit target type does not help.
 *
 * <p>What is kept is the date, and there is nothing about a date that a {@link Date} cannot hold.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose; see
 * {@link InstantToTimestampConverter} for why.
 */
public class LocalDateToSqlDateConverter extends LocalDateConverter {

    @Override
    public Object toDatabaseParam(LocalDate val) {
        return val == null ? null : Date.valueOf(val);
    }
}