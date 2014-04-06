package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;

/**
 * Represents a converter.
 */
public interface Converter<T> {
    
    T convert(Object val) throws ConverterException;

    void addParameter(StatementParameterSetter stmt, String name, T val) throws SQLException;

    Object toDatabaseParam(T val);
}
