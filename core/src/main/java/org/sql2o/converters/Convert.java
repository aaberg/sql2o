package org.sql2o.converters;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.sql2o.converters.joda.DateTimeConverter;
import org.sql2o.converters.joda.LocalDateConverter;
import org.sql2o.converters.joda.LocalTimeConverter;
import org.sql2o.tools.FeatureDetector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Static class used to register new converters.
 * Also used internally by sql2o to lookup a converter.
 */
@SuppressWarnings("unchecked")
public class Convert {

    private static final ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.ReadLock rl = rrwl.readLock();
    private static final ReentrantReadWriteLock.WriteLock wl = rrwl.writeLock();
    private static volatile EnumConverterFactory registeredEnumConverterFactory = new DefaultEnumConverterFactory();
    private static Map<Class<?>, Converter<?>> registeredConverters = new HashMap<Class<?>, Converter<?>>();

    private static void processProvider(ConvertersProvider convertersProvider) {
        convertersProvider.fill(registeredConverters);
    }

    private static void fillDefaults(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(Integer.class, new IntegerConverter(false));
        mapToFill.put(int.class, new IntegerConverter(true));

        mapToFill.put(Double.class, new DoubleConverter(false));
        mapToFill.put(double.class, new DoubleConverter(true));

        mapToFill.put(Float.class, new FloatConverter(false));
        mapToFill.put(float.class, new FloatConverter(true));

        mapToFill.put(Long.class, new LongConverter(false));
        mapToFill.put(long.class, new LongConverter(true));

        mapToFill.put(Short.class, new ShortConverter(false));
        mapToFill.put(short.class, new ShortConverter(true));

        mapToFill.put(Byte.class, new ByteConverter(false));
        mapToFill.put(byte.class, new ByteConverter(true));

        mapToFill.put(BigDecimal.class, new BigDecimalConverter());
        mapToFill.put(BigInteger.class, new BigIntegerConverter());

        mapToFill.put(String.class, new StringConverter());

        mapToFill.put(java.util.Date.class,DateConverter.instance);
        mapToFill.put(java.sql.Date.class,
                new AbstractDateConverter<java.sql.Date>(java.sql.Date.class) {
                    @Override
                    protected java.sql.Date fromMilliseconds(long millisecond) {
                        return new java.sql.Date(millisecond);
                    }
                });
        mapToFill.put(java.sql.Time.class,
                new AbstractDateConverter<java.sql.Time>(java.sql.Time.class) {
                    @Override
                    protected java.sql.Time fromMilliseconds(long millisecond) {
                        return new java.sql.Time(millisecond);
                    }
                });
        mapToFill.put(java.sql.Timestamp.class,
                new AbstractDateConverter<java.sql.Timestamp>(java.sql.Timestamp.class) {
                    @Override
                    protected java.sql.Timestamp fromMilliseconds(long millisecond) {
                        return new java.sql.Timestamp(millisecond);
                    }
                });

        BooleanConverter booleanConverter = new BooleanConverter();
        mapToFill.put(Boolean.class, booleanConverter);
        mapToFill.put(boolean.class, booleanConverter);

        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        //it's impossible to cast Byte[].class <-> byte[].class
        // and I'm too lazy to implement converter for Byte[].class
        // since it's really doesn't wide-used
        // otherwise someone already detect this error
        //mapToFill.put(Byte[].class, byteArrayConverter);
        mapToFill.put(byte[].class, byteArrayConverter);

        InputStreamConverter inputStreamConverter = new InputStreamConverter();
        mapToFill.put(InputStream.class, inputStreamConverter);
        mapToFill.put(ByteArrayInputStream.class, inputStreamConverter);

        mapToFill.put(UUID.class, new UUIDConverter());

        if (FeatureDetector.isJodaTimeAvailable()) {
            mapToFill.put(DateTime.class, new DateTimeConverter());
            mapToFill.put(LocalTime.class, new LocalTimeConverter());
            mapToFill.put(LocalDate.class, new LocalDateConverter());
        }
    }


    static {
        fillDefaults(registeredConverters);
        ServiceLoader<ConvertersProvider> loader = ServiceLoader.load(ConvertersProvider.class);
        for (ConvertersProvider provider : loader) {
            processProvider(provider);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static Converter getConverter(Class clazz) throws ConverterException {
        return throwIfNull(clazz, getConverterIfExists(clazz));
    }

    public static <E> Converter<E> throwIfNull(Class<E> clazz, Converter<E> converter) throws ConverterException {
        if (converter == null) {
            throw new ConverterException("No converter registered for class: " + clazz.getName());
        }
        return converter;
    }

    public static <E> Converter<E> getConverterIfExists(Class<E> clazz) {
        Converter c;
        rl.lock();
        try {
            c = registeredConverters.get(clazz);
        } finally {
            rl.unlock();
        }
        if (c != null) return c;

        if (clazz.isEnum()) {
            return registeredEnumConverterFactory.newConverter((Class) clazz);
        }
        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static void registerConverter(Class clazz, Converter converter) {
        wl.lock();
        try {
            registerConverter0(clazz, converter);
        } finally {
            wl.unlock();
        }
    }


    private static void registerConverter0(Class clazz, Converter converter) {
        registeredConverters.put(clazz, converter);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void registerEnumConverter(EnumConverterFactory enumConverterFactory) {
        if (enumConverterFactory == null) throw new IllegalArgumentException();
        registeredEnumConverterFactory = enumConverterFactory;
    }
}
