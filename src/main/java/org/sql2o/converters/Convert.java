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
        Converter integerConverter = new IntegerConverter();
        registerConverter(Integer.class, integerConverter);
        registerConverter(int.class, integerConverter);
        
        Converter doubleConverter = new DoubleConverter();
        registerConverter(Double.class, doubleConverter);
        registerConverter(double.class, doubleConverter);
        
        Converter floatConverter = new FloatConverter();
        registerConverter(Float.class, floatConverter);
        registerConverter(float.class, floatConverter);
        
        Converter longConverter = new LongConverter();
        registerConverter(Long.class, longConverter);
        registerConverter(long.class, longConverter);
        
        Converter shortConverter = new ShortConverter();
        registerConverter(Short.class, shortConverter);
        registerConverter(short.class, shortConverter);

        Converter byteConverter = new ByteConverter();
        registerConverter(Byte.class, byteConverter);
        registerConverter(byte.class, byteConverter);

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
