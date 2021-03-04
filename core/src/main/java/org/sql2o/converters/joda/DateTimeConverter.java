package org.sql2o.converters.joda;

import java.sql.Timestamp;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

/**
 * Used by sql2o to convert a value from the database into a {@link DateTime} instance.
 */
public class DateTimeConverter implements Converter<DateTime> {

    private final DateTimeZone timeZone;

    // it's possible to create instance for other timezone
    // and re-register converter
    public DateTimeConverter(DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public DateTimeConverter() {
        this(DateTimeZone.getDefault());
    }

    @Override
    public DateTime convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        try {
            // Joda has it's own pluggable converters infrastructure
            // it will throw IllegalArgumentException if can't convert
            // look @ org.joda.time.convert.ConverterManager
            return new LocalDateTime(val).toDateTime(timeZone);
        } catch (IllegalArgumentException ex) {
            throw new ConverterException("Error while converting type " + val.getClass().toString() + " to jodatime", ex);
        }
    }

    @Override
    public Object toDatabaseParam(DateTime val) {
        return new Timestamp(val.getMillis());
    }
}
