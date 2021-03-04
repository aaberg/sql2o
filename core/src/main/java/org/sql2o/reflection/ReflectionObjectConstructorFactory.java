package org.sql2o.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.sql2o.Sql2oException;

public class ReflectionObjectConstructorFactory implements ObjectConstructorFactory {
    @Override
    public ObjectConstructor newConstructor(final Class<?> clazz) {
        try {
            final Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return new ObjectConstructor() {
                @Override
                public Object newInstance() {
                    try {
                        return ctor.newInstance((Object[])null);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new Sql2oException("Could not create a new instance of class " + clazz, e);
                    }
                }
            };
        } catch (Throwable e) {
            throw new Sql2oException("Could not find parameter-less constructor of class " + clazz, e);
        }
    }
}
