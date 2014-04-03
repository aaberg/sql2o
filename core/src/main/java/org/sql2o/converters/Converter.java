package org.sql2o.converters;

/**
 * Represents a converter.
 */
public interface Converter<T> {
    
    T convert(Object val) throws ConverterException;
}
