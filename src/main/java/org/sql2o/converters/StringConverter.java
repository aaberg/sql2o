package org.sql2o.converters;

/**
 * Used by sql2o to convert a value from the database into a {@link String}.
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
