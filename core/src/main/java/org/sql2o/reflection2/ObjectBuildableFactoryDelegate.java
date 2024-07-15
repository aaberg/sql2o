package org.sql2o.reflection2;

public interface ObjectBuildableFactoryDelegate<T> {
    ObjectBuildable<T> newObjectBuilder();
}
