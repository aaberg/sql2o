package org.sql2o.reflection2;

import org.sql2o.NamingConvention;
import org.sql2o.Settings;

public class ObjectBuildableFactory {

    public static <T>ObjectBuildable<T> forClass(Class<T> targetClass, Settings settings) throws ReflectiveOperationException {

        if (targetClass.isRecord()) {
            return new RecordBuilder<>(targetClass, settings);
        }

        final var pojoMetadata = new PojoMetadata<>(targetClass, settings);
        return new PojoBuilder<>(targetClass, settings, pojoMetadata);
    }
}
