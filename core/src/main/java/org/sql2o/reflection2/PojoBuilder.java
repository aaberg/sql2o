package org.sql2o.reflection2;

import org.sql2o.Settings;

public class PojoBuilder<T> implements ObjectBuildable<T> {

    private final Settings settings;
    private final PojoMetadata<T> pojoMetadata;
    private final T pojo;

    public PojoBuilder(Class<T> pojoClass, Settings settings, PojoMetadata<T> pojoMetadata) throws ReflectiveOperationException {
        this.settings = settings;
        this.pojoMetadata = pojoMetadata;
        pojo = pojoMetadata.getConstructor().newInstance();
    }

    @Override
    public void withValue(String columnName, Object value) throws ReflectiveOperationException {
        final var derivedName = settings.getNamingConvention().deriveName(columnName);
        pojoMetadata.getPojoProperty(derivedName).SetProperty(this.pojo, value);
    }

    @Override
    public T build() throws ReflectiveOperationException {
        return pojo;
    }
}
