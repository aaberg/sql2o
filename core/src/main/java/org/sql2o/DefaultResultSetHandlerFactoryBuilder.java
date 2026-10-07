package org.sql2o;

import org.sql2o.quirks.Quirks;
import org.sql2o.reflection2.ObjectBuildable;
import org.sql2o.reflection2.ObjectBuildableFactory;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;
import org.sql2o.reflection2.PojoBuilder;
import org.sql2o.reflection2.PojoMetadata;
import org.sql2o.reflection2.RecordBuilder;

import java.util.HashMap;
import java.util.Map;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private boolean throwOnMappingError;
    // column mappings are optional, so the default is an empty map rather than null: the property lookup
    // dereferences this map, and a caller who never set any should not have to
    private Map<String, String> columnMappings = new HashMap<>();
    private Quirks quirks;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isAutoDeriveColumnNames() {
        return autoDeriveColumnNames;
    }

    public void setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
        this.autoDeriveColumnNames = autoDeriveColumnNames;
    }

    @Override
    public boolean isThrowOnMappingError() {
        return throwOnMappingError;
    }

    @Override
    public void throwOnMappingError(boolean throwOnMappingError) {
        this.throwOnMappingError = throwOnMappingError;
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
    }

    public Quirks getQuirks() {
        return quirks;
    }

    public void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

    public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
        // The settings are a property of the factory rather than of the row, so they are built once here. They used
        // to be rebuilt by every row, which also rehashed the metadata cache key on every row: a new Settings, a new
        // NamingConvention, a new key and two boxed varargs arrays, per row, for nothing.
        final Settings settings = new Settings(
            new NamingConvention(caseSensitive, autoDeriveColumnNames),
            quirks,
            throwOnMappingError);
        final boolean isRecord = clazz.isRecord();

        final ObjectBuildableFactoryDelegate<T> delegate = new ObjectBuildableFactoryDelegate<T>() {
            // Resolved on the first row rather than in newFactory, so that a class the metadata cannot be built for
            // fails when the first row is read, exactly as before, and not when the factory is created. The race is
            // benign: resolution is deterministic, and the static cache underneath returns the same metadata anyway.
            private volatile PojoMetadata<T> metadata;

            @Override
            public ObjectBuildable<T> newObjectBuilder() {
                try {
                    if (isRecord) {
                        return new RecordBuilder<>(clazz, settings);
                    }
                    PojoMetadata<T> resolved = metadata;
                    if (resolved == null) {
                        resolved = ObjectBuildableFactory.pojoMetadata(clazz, settings);
                        metadata = resolved;
                    }
                    return new PojoBuilder<>(settings, resolved, getColumnMappings());
                } catch (ReflectiveOperationException e) {
                    throw new Sql2oException("Error while trying to construct object from class " + clazz, e);
                }
            }
        };

        // The class and the settings let the factory resolve every column once, on the first row, instead of looking
        // the property and the converter up on every row. Records and dotted columns stay on the delegate above.
        return new DefaultResultSetHandlerFactory<>(delegate, quirks, clazz, settings, this::getColumnMappings);
    }

}
