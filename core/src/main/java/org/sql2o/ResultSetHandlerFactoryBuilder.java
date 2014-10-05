package org.sql2o;

import org.sql2o.quirks.Quirks;

import java.util.Map;
import java.util.Set;

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

    public Quirks getQuirks();

    public void setQuirks(Quirks quirksMode);

    public <E> ResultSetHandlerFactory<E> newFactory(Class<E> clazz);

	void setIgnoredColumns( Set<String> ignoredColumns );
}
