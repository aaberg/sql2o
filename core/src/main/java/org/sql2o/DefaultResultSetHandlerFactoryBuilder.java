package org.sql2o;

import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.PojoMetadata;

import java.util.Map;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
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

    @SuppressWarnings("unchecked")
    public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
        PojoMetadata pojoMetadata = new PojoMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings);
        return new DefaultResultSetHandlerFactory(pojoMetadata, quirks);
    }

}
