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
            if (subProperty == null) {
                handleMissingProperty(columnName);
                return;
            }
            final var newPath = columnName.substring(dotIdx + 1);

            var subObj = subProperty.getValue(this.pojo);
            if (subObj == null) {
                subObj = subProperty.initializeWithNewInstance(this.pojo);
                subProperty.SetProperty(this.pojo, subObj, settings.getQuirks());
            }

            // The nested object is filled in place instead of through a nested PojoBuilder. Its metadata
            // belongs to the runtime class of the value, while the value itself is only known as the declared
            // property type, so there is no type parameter that could tie a builder to that metadata: a nested
            // builder could only be created by giving up on generics, which is what the raw type fallback
            // used to do here.
            setNestedValue(subObj, newPath, obj);
            obj = subObj;
        }

        if (derivedName == null) {
            derivedName = settings.getNamingConvention().deriveName(columnName);
        }
        final var pojoProperty = pojoMetadata.getPojoProperty(derivedName, columnMappings);

        if (pojoProperty == null) {
            handleMissingProperty(columnName);
            return;
        }
        pojoProperty.SetProperty(this.pojo, obj, settings.getQuirks());
    }

    /**
     * Applies a column name, which may itself be dotted, to an object that already exists. The metadata is
     * resolved from the runtime class of the object, which is why this cannot go through a PojoBuilder.
     */
    private void setNestedValue(Object target, String columnName, Object value) throws ReflectiveOperationException {
        final var metadata = ObjectBuildableFactory.pojoMetadata(target.getClass(), settings);

        final var dotIdx = columnName.indexOf('.');
        if (dotIdx > 0) {
            final var subProperty = metadata.getPojoProperty(
                    settings.getNamingConvention().deriveName(columnName.substring(0, dotIdx)), columnMappings);
            if (subProperty == null) {
                handleMissingProperty(columnName);
                return;
            }

            var subTarget = subProperty.getValue(target);
            if (subTarget == null) {
                subTarget = subProperty.initializeWithNewInstance(target);
                subProperty.SetProperty(target, subTarget, settings.getQuirks());
            }

            setNestedValue(subTarget, columnName.substring(dotIdx + 1), value);
            return;
        }

        final var property = metadata.getPojoProperty(
                settings.getNamingConvention().deriveName(columnName), columnMappings);
        if (property == null) {
            handleMissingProperty(columnName);
            return;
        }

        property.SetProperty(target, value, settings.getQuirks());
    }

    /**
     * Reports a column that has no matching property, or silently ignores it when mapping errors are
     * not configured to be thrown.
     */
    private void handleMissingProperty(String columnName) {
        if (settings.isThrowOnMappingError()) {
            throw new Sql2oException("Could not map " + columnName + " to any property.");
        }
    }

    @Override
    public T build() throws ReflectiveOperationException {
        return pojo;
    }
}
