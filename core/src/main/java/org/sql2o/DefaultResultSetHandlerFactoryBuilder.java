package org.sql2o;

import java.util.Map;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.PojoMetadata;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private boolean throwOnMappingError;
    private Map<String, String> columnMappings;
    private Quirks quirks;

    @Override
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean isAutoDeriveColumnNames() {
        return autoDeriveColumnNames;
    }

    @Override
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

    @Override
    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    @Override
    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
    }

    @Override
    public Quirks getQuirks() {
        return quirks;
    }

    @Override
    public void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }



    @Override
    @SuppressWarnings("unchecked")
    public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
        PojoMetadata pojoMetadata = new PojoMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings, throwOnMappingError);
        return new DefaultResultSetHandlerFactory(pojoMetadata, quirks);
    }

}
