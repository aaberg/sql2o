package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.Sql2oException;

import java.util.Map;

public class PojoBuilder<T> implements ObjectBuildable<T> {

    private final Settings settings;
    private final PojoMetadata<T> pojoMetadata;
    private final T pojo;
    private final Map<String, String> columnMappings;

    public PojoBuilder(Settings settings, PojoMetadata<T> pojoMetadata, Map<String, String> columnMappings) throws ReflectiveOperationException {
        this(settings, pojoMetadata, columnMappings, pojoMetadata.getConstructor().newInstance());
    }

    public PojoBuilder(Settings settings, PojoMetadata<T> pojoMetadata, Map<String, String> columnMappings, T pojo) {
        this.settings = settings;
        this.pojoMetadata = pojoMetadata;
        this.columnMappings = columnMappings;
        this.pojo = pojo;
    }

    @Override
    public void withValue(String columnName, Object obj) throws ReflectiveOperationException {

        final var dotIdx = columnName.indexOf('.');
        String derivedName = null;
        if (dotIdx > 0) {
            final var subName = columnName.substring(0, dotIdx);
            derivedName = settings.getNamingConvention().deriveName(subName);
            final var subProperty = pojoMetadata.getPojoProperty(derivedName, columnMappings);
            final var newPath = columnName.substring(dotIdx + 1);

            var subObj = subProperty.getValue(this.pojo);
            if (subObj == null) {
                subObj = subProperty.initializeWithNewInstance(this.pojo);
                subProperty.SetProperty(this.pojo, subObj);
            }

            final var subPojoMetadata = new PojoMetadata<>(subObj.getClass(), settings);
            final var subObjectBuilder = new PojoBuilder(settings, subPojoMetadata, columnMappings, subObj);
            subObjectBuilder.withValue(newPath, obj);
            obj = subObjectBuilder.build();
        }

        if (derivedName == null) {
            derivedName = settings.getNamingConvention().deriveName(columnName);
        }
        final var pojoProperty = pojoMetadata.getPojoProperty(derivedName, columnMappings);

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
