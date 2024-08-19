package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.Sql2oException;

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
        final var pojoProperty = pojoMetadata.getPojoProperty(derivedName);

        if (pojoProperty == null) {
            if (settings.isThrowOnMappingError()){
                throw new Sql2oException("Could not map " + columnName + " to any property.");
            }
            return;
        }
        pojoProperty.SetProperty(this.pojo, value);
    }

    @Override
    public T build() throws ReflectiveOperationException {
        return pojo;
    }
}
