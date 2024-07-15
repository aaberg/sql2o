package org.sql2o.reflection2;

public interface ObjectBuildable<T> {

    void withValue(String columnName, Object value) throws ReflectiveOperationException;

    T build() throws ReflectiveOperationException;
}
