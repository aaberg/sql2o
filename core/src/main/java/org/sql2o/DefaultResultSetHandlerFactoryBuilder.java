package org.sql2o;

import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.reflection2.ObjectBuildable;
import org.sql2o.reflection2.ObjectBuildableFactory;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;

import java.util.Map;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private boolean throwOnMappingError;
    private Map<String, String> columnMappings;
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

        return new DefaultResultSetHandlerFactory<>(() -> {
            try {
                return ObjectBuildableFactory.forClass(clazz, new Settings(new NamingConvention(caseSensitive), quirks));
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error while trying to construct object from class " + clazz, e);
            }
        }, quirks);
    }

}
