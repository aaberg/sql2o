package org.sql2o.converters;

import org.sql2o.Sql2oException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DateConverter implements Converter<Date> {
    
    public Date convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        
        if (Date.class.isAssignableFrom( val.getClass() )){
            return (Date)val;
        }
        
        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Date");
    }
}
