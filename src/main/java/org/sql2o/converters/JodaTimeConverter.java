package org.sql2o.converters;

import org.joda.time.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class JodaTimeConverter implements Converter<DateTime> {
    public DateTime convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        
        try{
            return new DateTime(val);
        }
        catch(Throwable t){
            throw new ConverterException("Error while converting type " + val.getClass().toString() + " to jodatime", t);
        }
    }
}
