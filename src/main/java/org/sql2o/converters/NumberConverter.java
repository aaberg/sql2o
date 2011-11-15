package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class NumberConverter<V extends Number> implements Converter<V>{
    
    public V convert(Object val) {
        if (val == null){
            return null;
        }
        else if (val.getClass().isPrimitive() || Number.class.isAssignableFrom( val.getClass()) ) {
            return convertNumberValue((Number)val);
        }
        else if (val.getClass().equals(String.class)){
            return convertStringValue((String)val);
        }
        else{
            throw new IllegalArgumentException("Cannot convert type " + val.getClass().toString() + " to " + getTypeDescription());
        }
    }
    
    protected abstract V convertNumberValue(Number val);
    
    protected abstract V convertStringValue(String val);
    
    protected abstract String getTypeDescription();
}
