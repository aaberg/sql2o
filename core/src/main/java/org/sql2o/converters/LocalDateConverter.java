package org.sql2o.converters;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Used by sql2o to convert a value from the database into a {@link java.time.LocalDate}.
 *
 * @author Agit Rubar Demir | @agitrubard
 * @version 1.8.0
 * @since 13/2/2024
 */
public class LocalDateConverter implements Converter<LocalDate> {

    @Override
    public LocalDate convert(Object dateObject) throws ConverterException {

        if (dateObject == null) {
            return null;
        }

        try {
            return LocalDate.parse(dateObject.toString().split(" ")[0]);
        } catch (IllegalArgumentException exception) {
            String dateObjectClassName = dateObject.getClass().getName();
            String localDateClassName = LocalDate.class.getName();
            throw new ConverterException(
                String.format("Don't know how to convert from type '%s' to type '%s'", dateObjectClassName, localDateClassName),
                exception
            );
        }
    }

    @Override
    public Object toDatabaseParam(LocalDate localDate) {
        return Date.valueOf(localDate);
    }

}
