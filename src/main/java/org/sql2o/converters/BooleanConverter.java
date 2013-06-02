package org.sql2o.converters;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 6/1/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class BooleanConverter implements Converter<Boolean> {

    public Boolean convert(Object val) throws ConverterException {
        if (val == null) return null;

        if (Boolean.class.isAssignableFrom(val.getClass())) {
            return (Boolean) val;
        }

        if (Number.class.isAssignableFrom(val.getClass())) {
            return ((Number)val).intValue() > 0;
        }

        if (String.class.isAssignableFrom(val.getClass())) {
            String strVal = ((String)val).trim();
            return "Y".equalsIgnoreCase(strVal) || "YES".equalsIgnoreCase(strVal) || "TRUE".equalsIgnoreCase(strVal) ||
                    "T".equalsIgnoreCase(strVal) || "J".equalsIgnoreCase(strVal);
        }

        throw new ConverterException("Don't know how to convert type " + val.getClass().getName() + " to " + Boolean.class.getName());
    }
}
