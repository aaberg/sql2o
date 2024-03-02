package org.sql2o.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Used by sql2o to convert a value from the database into a {@link java.time.LocalDateTime}.
 *
 * @author Agit Rubar Demir | @agitrubard
 */
public class LocalDateTimeConverter implements Converter<LocalDateTime> {

    @Override
    public LocalDateTime convert(Object dateObject) throws ConverterException {

        if (dateObject == null) {
            return null;
        }

        try {
            return Timestamp.valueOf(dateObject.toString()).toLocalDateTime();
        } catch (IllegalArgumentException exception) {
            String dateObjectClassName = dateObject.getClass().getName();
            String localDateTimeClassName = LocalDateTime.class.getName();
            throw new ConverterException(
                String.format("Don't know how to convert from type '%s' to type '%s'", dateObjectClassName, localDateTimeClassName),
                exception
            );
        }
    }

    @Override
    public Object toDatabaseParam(LocalDateTime localDateTime) {
        return Timestamp.valueOf(localDateTime);
    }

}
