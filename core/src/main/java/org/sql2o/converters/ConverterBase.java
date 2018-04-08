package org.sql2o.converters;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
abstract class ConverterBase<T> implements Converter<T> {

    @Override
    public Object toDatabaseParam(T val) {
        return val;
    }
}
