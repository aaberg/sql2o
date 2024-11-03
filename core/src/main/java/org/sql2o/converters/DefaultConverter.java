package org.sql2o.converters;

public class DefaultConverter extends ConverterBase<Object> {
    @Override
    public Object convert(Object val) throws ConverterException {
        return val;
    }
}
