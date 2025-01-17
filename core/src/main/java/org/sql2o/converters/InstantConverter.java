package org.sql2o.converters;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantConverter extends ConverterBase<Instant> {
    @Override
    public Instant convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof Timestamp) {
            return ((Timestamp)val).toInstant();
        }
        if (val instanceof String) {
            try {
                return Instant.parse((String) val);
            }
            catch(DateTimeParseException e) {
                throw new ConverterException("Can't convert string with value '" + val + "' to java.time.Instant", e);
            }
        }
        if (val instanceof Long) {
            return Instant.ofEpochMilli((Long)val);
        }

        throw new ConverterException("Can't convert type " + val.getClass().getName() + " to Instant");
    }
}
