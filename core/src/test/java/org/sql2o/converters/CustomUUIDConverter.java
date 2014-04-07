package org.sql2o.converters;

import java.util.UUID;

/**
 * Stores UUIDs as strings with ! delimiter instead of -.
 *
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class CustomUUIDConverter implements Converter<UUID> {
    public UUID convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (String.class.isAssignableFrom(val.getClass())) {
            return UUID.fromString(((String)val).replace('!', '-'));
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " " + UUID.class);
    }

    public Object toDatabaseParam(UUID val) {
        return val.toString().replace('-', '!');
    }
}
