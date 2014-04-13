package org.sql2o.converters;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.sql2o.converters.joda.DateTimeConverter;
import org.sql2o.converters.joda.LocalTimeConverter;
import org.sql2o.tools.FeatureDetector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Static class used to register new converters.
 * Also used internally by sql2o to lookup a converter.
 */
public class Convert {

    private static final ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.ReadLock rl = rrwl.readLock();
    private static final ReentrantReadWriteLock.WriteLock wl = rrwl.writeLock();

    private static volatile EnumConverterFactory registeredEnumConverterFactory = new DefaultEnumConverterFactory();
    private static Map<Class, Converter> registeredConverters = new HashMap<Class, Converter>();

    static{
        registerConverter0(Integer.class, new IntegerConverter(false));
        registerConverter0(int.class, new IntegerConverter(true));

        registerConverter0(Double.class, new DoubleConverter(false));
        registerConverter0(double.class, new DoubleConverter(true));

        registerConverter0(Float.class, new FloatConverter(false));
        registerConverter0(float.class, new FloatConverter(true));

        registerConverter0(Long.class, new LongConverter(false));
        registerConverter0(long.class, new LongConverter(true));

        registerConverter0(Short.class, new ShortConverter(false));
        registerConverter0(short.class, new ShortConverter(true));

        registerConverter0(Byte.class, new ByteConverter(false));
        registerConverter0(byte.class, new ByteConverter(true));

        registerConverter0(BigDecimal.class, new BigDecimalConverter());

        registerConverter0(String.class, new StringConverter());

        registerConverter0(java.util.Date.class,DateConverter.instance);
        registerConverter0(java.sql.Date.class,
            new AbstractDateConverter<java.sql.Date>(java.sql.Date.class) {
                @Override
                protected java.sql.Date fromMilliseconds(long millisecond) {
                    return new java.sql.Date(millisecond);
                }
            });
        registerConverter0(java.sql.Time.class,
            new AbstractDateConverter<java.sql.Time>(java.sql.Time.class) {
                @Override
                protected java.sql.Time fromMilliseconds(long millisecond) {
                    return new java.sql.Time(millisecond);
                }
            });
        registerConverter0(java.sql.Timestamp.class,
            new AbstractDateConverter<java.sql.Timestamp>(java.sql.Timestamp.class) {
                @Override
                protected java.sql.Timestamp fromMilliseconds(long millisecond) {
                    return new java.sql.Timestamp(millisecond);
                }
            });

        BooleanConverter booleanConverter = new BooleanConverter();
        registerConverter0(Boolean.class, booleanConverter);
        registerConverter0(boolean.class, booleanConverter);

        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        //it's impossible to cast Byte[].class <-> byte[].class
        // and I'm too lazy to implement converter for Byte[].class
        // since it's really doesn't wide-used
        // otherwise someone already detect this error
        //registerConverter0(Byte[].class, byteArrayConverter);
        registerConverter0(byte[].class, byteArrayConverter);

        InputStreamConverter inputStreamConverter = new InputStreamConverter();
        registerConverter0(InputStream.class, inputStreamConverter);
        registerConverter0(ByteArrayInputStream.class, inputStreamConverter);

        registerConverter0(UUID.class, new UUIDConverter());

        if (FeatureDetector.isJodaTimeAvailable()) {
            registerConverter0(DateTime.class, new DateTimeConverter());
            registerConverter0(LocalTime.class, new LocalTimeConverter());
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
        Converter c;
        rl.lock();
        try{
            c = registeredConverters.get(clazz);
        } finally {
            rl.unlock();
        }
        if(c!=null) return c;

        if (clazz.isEnum()) {
          return registeredEnumConverterFactory.newConverter(clazz);
        }
       return null;
    }

    public static void registerConverter(Class clazz, Converter converter){
        wl.lock();
        try{
            registerConverter0(clazz, converter);
        } finally {
            wl.unlock();
        }
    }

    public static void registerConverter0(Class clazz, Converter converter){
        registeredConverters.put(clazz, converter);
    }

    public static void registerEnumConverter(EnumConverterFactory enumConverterFactory) {
        if(enumConverterFactory==null) throw new IllegalArgumentException();
        registeredEnumConverterFactory = enumConverterFactory;
    }
}
