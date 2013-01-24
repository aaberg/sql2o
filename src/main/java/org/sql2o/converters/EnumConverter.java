package org.sql2o.converters;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 1/22/13
 * Time: 10:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnumConverter implements Converter<Enum> {

    private final Class enumType;

    public EnumConverter(Class enumType) {
        this.enumType = enumType;
    }

    public Enum convert(Object val) throws ConverterException {
        if (val == null) return null;
        try{
            if (String.class.isAssignableFrom(val.getClass())){
                return Enum.valueOf(enumType, val.toString());
            } else if (Number.class.isAssignableFrom(val.getClass())){
                return (Enum)enumType.getEnumConstants()[((Number)val).intValue()];
            }
        } catch (Throwable t) {
            throw new ConverterException("Error converting value '" + val.toString() + "' to " + enumType.getName(), t);
        }
        throw new ConverterException("Cannot convert type '" + val.getClass().getName() + "' to an Enum");
    }
}
