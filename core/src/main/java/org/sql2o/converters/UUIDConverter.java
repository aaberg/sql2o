package org.sql2o.converters;

import org.sql2o.Sql2oException;

import java.util.UUID;

/**
 * Used by sql2o to convert a value from the database into a {@link UUID}.
 */
public class UUIDConverter implements Converter<UUID> {

    public UUID convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (UUID.class.isAssignableFrom( val.getClass() )){
            return (UUID)val;
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.UUID");
    }
}

