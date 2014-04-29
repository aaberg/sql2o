package org.sql2o.converters;

/**
 * Default enum converter,
 * used by sql2o to convert a value from the database into an {@link Enum}.
 */
public class DefaultEnumConverter implements Converter<Enum>
{
    private final Class enumType;

    public DefaultEnumConverter(Class enumType) {
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
