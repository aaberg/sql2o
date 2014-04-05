package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("Unsafe")
public class FactoryFacade {
    private final FieldSetterFactory fieldSetterFactory;
    private final MethodSetterFactory methodSetterFactory;
    private final static FactoryFacade instance;

    static {
        MethodSetterFactory m = new ReflectionMethodSetterFactory();
        FieldSetterFactory f;
        try{
            Class cls = Class.forName("org.sql2o.reflection.UnsafeFieldSetterFactory");
            f = (FieldSetterFactory) cls.newInstance();
        } catch (Throwable ex){
            f = new ReflectionFieldSetterFactory();
        }
        instance = new FactoryFacade(f, m);
    }

    public FactoryFacade(FieldSetterFactory fieldSetterFactory, MethodSetterFactory methodSetterFactory) {
        this.fieldSetterFactory = fieldSetterFactory;
        this.methodSetterFactory = methodSetterFactory;
    }

    public Setter newSetter(Field field) {
        return fieldSetterFactory.newSetter(field);
    }

    public Setter newSetter(Method method) {
        return methodSetterFactory.newSetter(method);
    }

    public static FactoryFacade getInstance() {
        return instance;
    }
}

