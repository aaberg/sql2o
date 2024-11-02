package org.sql2o.converters;

/**
 * Base class for numeric converters.
 */
public abstract class NumberConverter<V extends Number> extends ConverterBase<V> {

    private final boolean isPrimitive;

    public NumberConverter(boolean primitive) {
        isPrimitive = primitive;
    }

    public V convert(Object val) {
        if (val == null) {
            return isPrimitive ? convertNumberValue(0) : null;
        }

        else if (val instanceof Number num) {
            return convertNumberValue(num);
        }
        else if (val instanceof String strVal){
            strVal = strVal.trim();

            if (strVal.isEmpty()) {
                return isPrimitive ? convertNumberValue(0) : null;
            }
            return convertStringValue(strVal);
        }
        else{
            throw new IllegalArgumentException("Cannot convert type " + val.getClass().toString() + " to " + getTypeDescription());
        }
    }

    protected abstract V convertNumberValue(Number val);
    
    protected abstract V convertStringValue(String val);
    
    protected abstract String getTypeDescription();
}
