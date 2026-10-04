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
     * <p>The naming convention is part of the key because the metadata derives its property names from it, so
     * metadata built for one convention must not be handed out for another one. The quirks are not part of
     * the key, because nothing in the metadata uses them: they are passed to
     * {@link PojoProperty#SetProperty(Object, Object, org.sql2o.quirks.Quirks)} by the builder.
     */
    static <T> PojoMetadata<T> pojoMetadata(Class<T> targetClass, Settings settings) {
        final var key = new MetadataKey(targetClass, settings.getNamingConvention());

        //noinspection unchecked
        return (PojoMetadata<T>) pojoMetadataCache.get(key, () -> new PojoMetadata<>(targetClass, settings));
    }

    public static <T>ObjectBuildable<T> forClass(Class<T> targetClass, Settings settings, Map<String, String> columnMappings) throws ReflectiveOperationException {

        if (targetClass.isRecord()) {
            return new RecordBuilder<>(targetClass, settings);
        }

        return new PojoBuilder<>(settings, pojoMetadata(targetClass, settings), columnMappings);
    }
}
