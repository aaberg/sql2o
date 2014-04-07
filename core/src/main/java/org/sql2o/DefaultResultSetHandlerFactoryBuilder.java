package org.sql2o;

import org.sql2o.reflection.PojoMetadata;

import java.util.Map;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private Map<String, String> columnMappings;
    private QuirksMode quirksMode;

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

    public QuirksMode getQuirksMode() {
        return quirksMode;
    }

    public void setQuirksMode(QuirksMode quirksMode) {
        this.quirksMode = quirksMode;
    }

    public ResultSetHandlerFactory newFactory(Class clazz) {
        PojoMetadata pojoMetadata = new PojoMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings);
        return new DefaultResultSetHandlerFactory(pojoMetadata, quirksMode);
    }

}
