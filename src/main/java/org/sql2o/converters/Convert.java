package org.sql2o.converters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Static class used to register new converters. Also used internally by sql2o to lookup a converter.
 */
public class Convert {

    private static final Logger logger = LoggerFactory.getLogger(Convert.class);
    
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

        BooleanConverter booleanConverter = new BooleanConverter();
        registerConverter(Boolean.class, booleanConverter);
        registerConverter(boolean.class, booleanConverter);

        try {
            Class jodaTimeClass = Class.forName("org.joda.time.DateTime");
            registerConverter(jodaTimeClass, new JodaTimeConverter());
        } catch (ClassNotFoundException e) {
            logger.warn("Failed to initialize Jodatime. Jodatime converter not registered");
        }

        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        registerConverter(Byte[].class, byteArrayConverter);
        registerConverter(byte[].class, byteArrayConverter);

        InputStreamConverter inputStreamConverter = new InputStreamConverter();
        registerConverter(InputStream.class, inputStreamConverter);
        registerConverter(ByteArrayInputStream.class, inputStreamConverter);

        registerConverter(UUID.class, new UUIDConverter());

    }
    
    public static Converter getConverter(Class clazz) throws ConverterException {
        if (registeredConverters.containsKey(clazz)){
            return registeredConverters.get(clazz);
        } else if (clazz.isEnum()) {
            return new EnumConverter(clazz);
        } else{
            throw new ConverterException("No converter registered for class: " + clazz.getName());
        }

    }
    
    public static void registerConverter(Class clazz, Converter converter){
        registeredConverters.put(clazz, converter);
    }
}
