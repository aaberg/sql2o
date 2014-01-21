package org.sql2o.converters;

/**
 * Used by sql2o to convert a value from the database into an {@link Enum}.
 */
public interface EnumConverter extends Converter<Enum> {
    void setEnumType(Class clazz);
}
