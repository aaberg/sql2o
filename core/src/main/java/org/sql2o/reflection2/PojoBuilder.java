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
        setValue(pojoMetadata, this.pojo, columnName, obj);
    }

    /**
     * Applies a column name, which may itself be dotted, to the given object, creating and assigning the
     * intermediate objects it walks through.
     *
     * <p>The object is passed along instead of a nested PojoBuilder because the metadata of a nested object
     * belongs to the runtime class of the value, while the value itself is only known as the declared
     * property type. There is no type parameter that could tie a builder to that metadata.
     */
    private void setValue(PojoMetadata<?> metadata, Object target, String columnName, Object value)
            throws ReflectiveOperationException {

        final var dotIdx = columnName.indexOf('.');
        final var head = dotIdx > 0 ? columnName.substring(0, dotIdx) : columnName;

        final var property = metadata.getPojoProperty(settings.getNamingConvention().deriveName(head), columnMappings);
        if (property == null) {
            handleMissingProperty(columnName);
            return;
        }

        if (dotIdx <= 0) {
            property.SetProperty(target, value, settings.getQuirks());
            return;
        }

        Object nested = property.getValue(target);
        if (nested == null) {
            // initializeWithNewInstance assigns the new instance to the target itself, calling the setter
            // or setting the field, so assigning it again here would invoke the setter twice.
            nested = property.initializeWithNewInstance(target);
        }

        setValue(ObjectBuildableFactory.pojoMetadata(nested.getClass(), settings), nested, columnName.substring(dotIdx + 1), value);
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
