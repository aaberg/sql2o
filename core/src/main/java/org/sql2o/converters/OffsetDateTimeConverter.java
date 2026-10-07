package org.sql2o.converters;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class OffsetDateTimeConverter extends ConverterBase<OffsetDateTime>{

    /**
     * The offset the jvm has right now. Asked for on its own rather than through {@code ZoneOffset.systemDefault()},
     * which does not exist and silently resolves to the inherited {@link ZoneId} one, returning the wrong type.
     */
    private static ZoneOffset currentOffset() {
        return OffsetDateTime.now(ZoneId.systemDefault()).getOffset();
    }

    @Override
    public OffsetDateTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }
        if (val instanceof OffsetDateTime) {
            return (OffsetDateTime) val;
        }

        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toInstant().atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
        }

        // A date has no time of day, and midnight is the only reading of it that does not invent anything.
        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toLocalDate().atStartOfDay().atOffset(currentOffset());
        }

        if (val instanceof Long) {
            final var instant = Instant.ofEpochMilli((Long)val);
            return instant.atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
        }

        if (val instanceof String) {
            try {
                return OffsetDateTime.parse((String) val);
            } catch (Exception e) {
                throw new ConverterException("Cannot convert String with value '" + val+ "' to java.time.OffsetDateTime", e);
            }
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.time.OffsetDateTime");
    }
}
