package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("Unsafe")
public class FactoryFacade {
    private final FieldSetterFactory fieldSetterFactory;
    private final MethodSetterFactory methodSetterFactory;
    private final ObjectConstructorFactory objectConstructorFactory;
    private final static FactoryFacade instance;

    static {
        MethodSetterFactory m = new ReflectionMethodSetterFactory();
        FieldSetterFactory f;
        ObjectConstructorFactory o;
        try{
            Class cls = Class.forName("org.sql2o.reflection.UnsafeFieldSetterFactory");
            f = (FieldSetterFactory) cls.newInstance();
            o = (ObjectConstructorFactory) f;
        } catch (Throwable ex){
            f = new ReflectionFieldSetterFactory();
            o = new ReflectionObjectConstructorFactory();
        }
        instance = new FactoryFacade(f, m, o);
    }

    public FactoryFacade(FieldSetterFactory fieldSetterFactory, MethodSetterFactory methodSetterFactory, ObjectConstructorFactory objectConstructorFactory) {
        this.fieldSetterFactory = fieldSetterFactory;
        this.methodSetterFactory = methodSetterFactory;
        this.objectConstructorFactory = objectConstructorFactory;
    }

    public Setter newSetter(Field field) {
        return fieldSetterFactory.newSetter(field);
    }

    public Setter newSetter(Method method) {
        return methodSetterFactory.newSetter(method);
    }

    public ObjectConstructor newConstructor(Class<?> cls) {
        return objectConstructorFactory.newConstructor(cls);
    }

    public static FactoryFacade getInstance() {
        return instance;
    }
}

