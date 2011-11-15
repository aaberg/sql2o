package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringConverter implements Converter<String>{

    public String convert(Object val) {
        if (val == null){
            return null;
        }
        else{
            return val.toString();
        }
    }
}
