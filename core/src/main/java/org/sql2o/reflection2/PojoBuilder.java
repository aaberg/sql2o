package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.Sql2oException;

public class PojoBuilder<T> implements ObjectBuildable<T> {

    private final Settings settings;
    private final PojoMetadata<T> pojoMetadata;
    private final T pojo;

    public PojoBuilder(Settings settings, PojoMetadata<T> pojoMetadata) throws ReflectiveOperationException {
        this(settings, pojoMetadata, pojoMetadata.getConstructor().newInstance());
    }

    public PojoBuilder(Settings settings, PojoMetadata<T> pojoMetadata, T pojo) {
        this.settings = settings;
        this.pojoMetadata = pojoMetadata;
        this.pojo = pojo;
    }

    @Override
    public void withValue(String columnName, Object obj) throws ReflectiveOperationException {

        final var dotIdx = columnName.indexOf('.');
        String derivedName = null;
        if (dotIdx > 0) {
            final var subName = columnName.substring(0, dotIdx);
            derivedName = settings.getNamingConvention().deriveName(subName);
            final var subProperty = pojoMetadata.getPojoProperty(derivedName);
            final var newPath = columnName.substring(dotIdx + 1);

            var subObj = subProperty.getValue(this.pojo);
            if (subObj == null) {
                subObj = subProperty.initializeWithNewInstance(this.pojo);
                subProperty.SetProperty(this.pojo, subObj);
            }

            final var subPojoMetadata = new PojoMetadata<>(subObj.getClass(), settings, pojoMetadata.getColumnMappings());
            final var subObjectBuilder = new PojoBuilder(settings, subPojoMetadata, subObj);
            subObjectBuilder.withValue(newPath, obj);
            obj = subObjectBuilder.build();
        }

        if (derivedName == null) {
            derivedName = settings.getNamingConvention().deriveName(columnName);
        }
        final var pojoProperty = pojoMetadata.getPojoProperty(derivedName);

        if (pojoProperty == null) {
            if (settings.isThrowOnMappingError()){
                throw new Sql2oException("Could not map " + columnName + " to any property.");
            }
            return;
        }
        pojoProperty.SetProperty(this.pojo, obj);
    }

    @Override
    public T build() throws ReflectiveOperationException {
        return pojo;
    }
}
