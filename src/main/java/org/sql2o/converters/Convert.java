package org.sql2o.converters;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;
import org.sql2o.tools.ClassUtils;
import org.sql2o.tools.FeatureDetector;

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

    private static final Logger logger = LocalLoggerFactory.getLogger(Convert.class);

    private static EnumConverter registeredEnumConverter = new DefaultEnumConverter();
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

        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        registerConverter(Byte[].class, byteArrayConverter);
        registerConverter(byte[].class, byteArrayConverter);

        InputStreamConverter inputStreamConverter = new InputStreamConverter();
        registerConverter(InputStream.class, inputStreamConverter);
        registerConverter(ByteArrayInputStream.class, inputStreamConverter);

        registerConverter(UUID.class, new UUIDConverter());

        if (FeatureDetector.isJodaTimeAvailable()) {
            registerConverter(DateTime.class, new JodaTimeConverter());
            registerConverter(LocalTime.class, new LocalTimeConverter());
        }
    }

    public static Converter getConverter(Class clazz) throws ConverterException {
        Converter converter = getConverterIfExists(clazz);
        if (converter == null) {
            throw new ConverterException("No converter registered for class: " + clazz.getName());
        }
        return converter;
    }

    public static Converter getConverterIfExists(Class clazz) {
        if (registeredConverters.containsKey(clazz)){
            return registeredConverters.get(clazz);
        } else if (clazz.isEnum()) {
            registeredEnumConverter.setEnumType(clazz);
            return registeredEnumConverter;
        } else {
            return null;
        }
    }

    public static void registerConverter(Class clazz, Converter converter){
        registeredConverters.put(clazz, converter);
    }

    public static void registerEnumConverter(EnumConverter converter) {
        registeredEnumConverter = converter;
    }
}
