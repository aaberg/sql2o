package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.tools.Cache;

import java.util.Map;

public class ObjectBuildableFactory {

    private static final Cache<Class<?>, PojoMetadata<?>> pojoMetadataCache = new Cache<>();

    public static <T>ObjectBuildable<T> forClass(Class<T> targetClass, Settings settings, Map<String, String> columnMappings) throws ReflectiveOperationException {

        if (targetClass.isRecord()) {
            return new RecordBuilder<>(targetClass, settings);
        }

        //noinspection unchecked
        final var pojoMetadata = (PojoMetadata<T>) pojoMetadataCache.get(targetClass, () ->
            new PojoMetadata<>(targetClass, settings));

        return new PojoBuilder<>(settings, pojoMetadata, columnMappings);
    }
}
