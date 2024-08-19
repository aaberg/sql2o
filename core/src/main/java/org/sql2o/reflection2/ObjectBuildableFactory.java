package org.sql2o.reflection2;

import org.sql2o.Settings;

import java.util.Map;

public class ObjectBuildableFactory {

    public static <T>ObjectBuildable<T> forClass(Class<T> targetClass, Settings settings, Map<String, String> columnMappings) throws ReflectiveOperationException {

        if (targetClass.isRecord()) {
            return new RecordBuilder<>(targetClass, settings);
        }

        final var pojoMetadata = new PojoMetadata<>(targetClass, settings, columnMappings);
        return new PojoBuilder<>(targetClass, settings, pojoMetadata);
    }
}
