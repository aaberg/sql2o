package org.sql2o.reflection;

import org.sql2o.Sql2oException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionObjectConstructorFactory implements ObjectConstructorFactory {
    public ObjectConstructor newConstructor(final Class<?> clazz) {
        try {
            final Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return new ObjectConstructor() {
                public Object newInstance() {
                    try {
                        return ctor.newInstance((Object[])null);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new Sql2oException("Could not create a new instance of class " + clazz, e);
                    }
                }
            };
        } catch (Throwable e) {
            if (clazz.getEnclosingClass() == null) {
                throw new Sql2oException("Could not find parameter-less constructor of class " + clazz, e);
            } else {
                throw new Sql2oException("Could not find parameter-less constructor of class " + clazz + ", if your pojo is a nested class, you could try to make it static.", e);
            }
        }
    }
}
