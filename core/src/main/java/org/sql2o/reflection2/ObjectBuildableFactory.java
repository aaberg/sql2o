package org.sql2o.reflection2;

import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.tools.Cache;

import java.util.Map;

public class ObjectBuildableFactory {

    private record MetadataKey(Class<?> clazz, NamingConvention namingConvention) {}

    private static final Cache<MetadataKey, PojoMetadata<?>> pojoMetadataCache = new Cache<>();

    /**
     * Returns the metadata for the given class, building it the first time it is asked for.
     *
     * <p>Public so that a mapping implementation living outside this package can reach the same metadata rather than
     * introspecting the class again: building it walks the declared methods and fields of the class and its
     * superclasses and reads the annotations off them, which is work worth doing once per class rather than once per
     * row. The bytecode extension in {@code extensions/bytecode} is the reason this is part of the api rather than an
     * implementation detail of {@link PojoBuilder}.
     *
     * <p>The naming convention is part of the key because the metadata derives its property names from it, so
     * metadata built for one convention must not be handed out for another one. The quirks are not part of
     * the key, because nothing in the metadata uses them: they are passed to
     * {@link PojoProperty#SetProperty(Object, Object, org.sql2o.quirks.Quirks)} by the builder.
     *
     * <p>The cast is the one place where the type parameter cannot be proven. The cache is static and
     * therefore holds metadata for many classes, so its value type is PojoMetadata&lt;?&gt; and javac cannot
     * connect that back to the requested type. It is safe because the key holds exactly the class the
     * delegate builds the metadata from, so a cache hit can only return metadata that was created for that
     * same class, and PojoMetadata keeps nothing that depends on its type parameter apart from the
     * Constructor taken from that class. The unchecked value never leaves this method as anything but a
     * PojoMetadata of the requested type.
     */
    @SuppressWarnings("unchecked")
    public static <T> PojoMetadata<T> pojoMetadata(Class<T> targetClass, Settings settings) {
        final var key = new MetadataKey(targetClass, settings.getNamingConvention());

        return (PojoMetadata<T>) pojoMetadataCache.get(key, () -> new PojoMetadata<>(targetClass, settings));
    }

    public static <T>ObjectBuildable<T> forClass(Class<T> targetClass, Settings settings, Map<String, String> columnMappings) throws ReflectiveOperationException {

        if (targetClass.isRecord()) {
            return new RecordBuilder<>(targetClass, settings);
        }

        return new PojoBuilder<>(settings, pojoMetadata(targetClass, settings), columnMappings);
    }
}
