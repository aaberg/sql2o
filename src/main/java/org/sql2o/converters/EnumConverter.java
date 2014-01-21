package org.sql2o.converters;

/**
 * Class description.
 *
 * @author alden@mark43.com
 */
public interface EnumConverter extends Converter<Enum> {
    void setEnumType(Class clazz);
}
