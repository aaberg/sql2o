package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import sun.reflect.ConstructorAccessor;
import sun.reflect.FieldAccessor;
import sun.reflect.MethodAccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("UnusedDeclaration")
public class MethodAccessorsGenerator implements MethodSetterFactory, ObjectConstructorFactory {
    private static final Object generatorObject;
    private static final MethodAccessor generateMethod;
    private static final MethodAccessor generateConstructor;
    private static final MethodAccessor newFieldAccessor;

    static {
        try {
            Class<?> aClass = Class.forName("sun.reflect.MethodAccessorGenerator");
            Constructor<?>[] declaredConstructors = aClass.getDeclaredConstructors();
            Constructor<?> declaredConstructor = declaredConstructors[0];
            declaredConstructor.setAccessible(true);
            generatorObject = declaredConstructor.newInstance();
            Method bar = aClass.getMethod("generateMethod", Class.class, String.class, Class[].class, Class.class, Class[].class, Integer.TYPE);
            bar.setAccessible(true);
            generateMethod = (MethodAccessor) bar.invoke(
                    generatorObject,
                    bar.getDeclaringClass(),
                    bar.getName(),
                    bar.getParameterTypes(),
                    bar.getReturnType(),
                    bar.getExceptionTypes(),
                    bar.getModifiers());
            bar = aClass.getMethod("generateConstructor", Class.class, Class[].class, Class[].class, Integer.TYPE);
            generateConstructor = newMethodAccessor(bar);
            aClass = Class.forName("sun.reflect.UnsafeFieldAccessorFactory");
            bar = aClass.getDeclaredMethod("newFieldAccessor", Field.class, Boolean.TYPE);
            newFieldAccessor = newMethodAccessor(bar);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static FieldAccessor newFieldAccessor(Field field, boolean overrideFinalCheck) {
        try {
            return (FieldAccessor) newFieldAccessor.invoke(null, new Object[]{field, overrideFinalCheck});
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodAccessor newMethodAccessor(Method bar) {
        try {
            return (MethodAccessor) generateMethod.invoke(
                    generatorObject, new Object[]{
                    bar.getDeclaringClass(),
                    bar.getName(),
                    bar.getParameterTypes(),
                    bar.getReturnType(),
                    bar.getExceptionTypes(),
                    bar.getModifiers()});
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConstructorAccessor newConstructorAccessor(Constructor<?> bar) {
        try {
            return (ConstructorAccessor) generateConstructor.invoke(
                    generatorObject, new Object[]{
                    bar.getDeclaringClass(),
                    bar.getParameterTypes(),
                    bar.getExceptionTypes(),
                    bar.getModifiers()});
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Setter newSetter(final Method method) {
        final Class type = method.getParameterTypes()[0];
        final MethodAccessor methodAccessor = newMethodAccessor(method);
        return new Setter() {
            public void setProperty(Object obj, Object value) {
                if (value == null && type.isPrimitive()) return;
                try {
                    methodAccessor.invoke(obj, new Object[]{value});
                } catch (InvocationTargetException e) {
                    throw new Sql2oException("error while calling setter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
                }
            }
            public Class getType() {
                return type;
            }
        };
    }


    @Override
    public ObjectConstructor newConstructor(final Class<?> cls) {
        try {
            final Constructor<?> constructor = cls.getDeclaredConstructor();
            final ConstructorAccessor constructorAccessor = newConstructorAccessor(constructor);
            return new ObjectConstructor() {
                @Override
                public Object newInstance() {
                    try {
                        return constructorAccessor.newInstance((Object[])null);
                    } catch (InstantiationException | InvocationTargetException e) {
                        throw new Sql2oException("Could not create a new instance of class " + cls, e);
                    }
                }
            };
        } catch (NoSuchMethodException e) {
            return UnsafeFieldSetterFactory.getConstructor(cls);
        }
    }
}