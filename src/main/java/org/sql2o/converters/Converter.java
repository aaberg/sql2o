package org.sql2o.converters;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Converter<T> {
    
    T convert(Object val) throws ConverterException;
}
