package org.sql2o.converters;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Convert {

    private static Map<Class, Converter> registeredConverters = new HashMap<Class, Converter>();

    static{
        registerConverter(Integer.class, new IntegerConverter(false));
        registerConverter(int.class, new IntegerConverter(true));

        registerConverter(Double.class, new DoubleConverter(false));
        registerConverter(double.class, new DoubleConverter(true));

        registerConverter(Float.class, new FloatConverter(false));
        registerConverter(float.class, new FloatConverter(true));

        registerConverter(Long.class, new LongConverter(false));
        registerConverter(long.class, new LongConverter(true));

        registerConverter(Short.class, new ShortConverter(false));
        registerConverter(short.class, new ShortConverter(true));

        registerConverter(Byte.class, new ByteConverter(false));
        registerConverter(byte.class, new ByteConverter(true));

        registerConverter(BigDecimal.class, new BigDecimalConverter());
        
        registerConverter(String.class, new StringConverter());
        
        Converter utilDateConverter = new DateConverter();
        registerConverter(java.util.Date.class, utilDateConverter);
        registerConverter(java.sql.Date.class, utilDateConverter);
        registerConverter(java.sql.Time.class, utilDateConverter);
        registerConverter(java.sql.Timestamp.class, utilDateConverter);

        try {
            Class jodaTimeClass = Class.forName("org.joda.time.DateTime");
            registerConverter(jodaTimeClass, new JodaTimeConverter());
        } catch (ClassNotFoundException e) {
            System.out.print("Failed to initialize Jodatime. Jodatime converter not registered");
        }
    }
    
    public static Converter getConverter(Class clazz) throws ConverterException {
        if (registeredConverters.containsKey(clazz)){
            return registeredConverters.get(clazz);
        }
        else{
            throw new ConverterException("No converter registered for class: " + clazz.toString());
        }

    }
    
    public static void registerConverter(Class clazz, Converter converter){
        registeredConverters.put(clazz, converter);
    }
}
