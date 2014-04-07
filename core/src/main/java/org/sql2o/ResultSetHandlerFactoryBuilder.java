package org.sql2o;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dimzon
 * Date: 4/7/14
 * Time: 4:28 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ResultSetHandlerFactoryBuilder {
    public boolean isCaseSensitive();

    public void setCaseSensitive(boolean caseSensitive);

    public boolean isAutoDeriveColumnNames();

    public void setAutoDeriveColumnNames(boolean autoDeriveColumnNames);

    public Map<String, String> getColumnMappings();

    public void setColumnMappings(Map<String, String> columnMappings);

    public QuirksMode getQuirksMode();

    public void setQuirksMode(QuirksMode quirksMode);

    public <E> ResultSetHandlerFactory<E> newFactory(Class<E> clazz);
}
