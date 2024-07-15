package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("Unsafe")
public class FactoryFacade {
    private final static FactoryFacade instance;

    static {
        MethodGetterFactory mg = new ReflectionMethodGetterFactory();
        MethodSetterFactory m = new ReflectionMethodSetterFactory();
        ObjectConstructorFactory o = new ReflectionObjectConstructorFactory();
        FieldGetterFactory fg = new ReflectionFieldGetterFactory();
        FieldSetterFactory f = new ReflectionFieldSetterFactory();
        instance = new FactoryFacade(fg, mg, f, m, o);
    }

    private final FieldGetterFactory fieldGetterFactory;
    private final MethodGetterFactory methodGetterFactory;
    private final FieldSetterFactory fieldSetterFactory;
    private final MethodSetterFactory methodSetterFactory;
    private final ObjectConstructorFactory objectConstructorFactory;

    public FactoryFacade(
        FieldGetterFactory fieldGetterFactory, MethodGetterFactory methodGetterFactory,
        FieldSetterFactory fieldSetterFactory, MethodSetterFactory methodSetterFactory,
        ObjectConstructorFactory objectConstructorFactory) {

        this.fieldGetterFactory = fieldGetterFactory;
        this.methodGetterFactory = methodGetterFactory;
        this.fieldSetterFactory = fieldSetterFactory;
        this.methodSetterFactory = methodSetterFactory;
        this.objectConstructorFactory = objectConstructorFactory;
    }

    public static FactoryFacade getInstance() {
        return instance;
    }

    public Getter newGetter(Field field) {
        return fieldGetterFactory.newGetter(field);
    }

    public Getter newGetter(Method method) {
        return methodGetterFactory.newGetter(method);
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
}

