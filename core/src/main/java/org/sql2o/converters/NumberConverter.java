package org.sql2o.converters;

/**
 * Base class for numeric converters.
 */
public abstract class NumberConverter<V extends Number> implements Converter<V>{

    public NumberConverter(boolean primitive) {
        isPrimitive = primitive;
    }

    public V convert(Object val) {
        if (val == null){
            return null;
        }
        else if (val.getClass().isPrimitive() || Number.class.isAssignableFrom( val.getClass()) ) {
            return convertNumberValue((Number)val);
        }
        else if (val.getClass().equals(String.class)){
            String stringVal = ((String)val).trim();
            stringVal = stringVal.isEmpty() ? null : stringVal;

            if (stringVal == null && isPrimitive){
                return convertNumberValue(0);
            }
            else if (stringVal == null){
                return null;
            }

            return convertStringValue(stringVal);
        }
        else{
            throw new IllegalArgumentException("Cannot convert type " + val.getClass().toString() + " to " + getTypeDescription());
        }
    }
    
    protected abstract V convertNumberValue(Number val);
    
    protected abstract V convertStringValue(String val);
    
    protected abstract String getTypeDescription();
    
    private boolean isPrimitive;
}
