package org.sql2o.converters;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
abstract class BuiltInConverterBase<T> implements Converter<T> {

    public Object toDatabaseParam(T val) {
        return val;
    }
}
