package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("Unsafe")
public class FactoryFacade {
    private final static FactoryFacade instance;

    static {
        MethodGetterFactory mg;
        MethodSetterFactory m;
        ObjectConstructorFactory o;
        try {
            m = (MethodSetterFactory) Class
                    .forName("org.sql2o.reflection.MethodAccessorsGenerator")
                    .newInstance();
            mg = (MethodGetterFactory) m;
            o = (ObjectConstructorFactory) m;
        } catch (Throwable ex) {
            mg = new ReflectionMethodGetterFactory();
            m = new ReflectionMethodSetterFactory();
            o = null;
        }
        FieldGetterFactory fg;
        FieldSetterFactory f;
        try {
            Class clsg = Class.forName("org.sql2o.reflection.UnsafeFieldGetterFactory");
            fg = (FieldGetterFactory) clsg.newInstance();
            Class cls = Class.forName("org.sql2o.reflection.UnsafeFieldSetterFactory");
            f = (FieldSetterFactory) cls.newInstance();
            if(o==null) o = (ObjectConstructorFactory) f;
        } catch (Throwable ex) {
            fg = new ReflectionFieldGetterFactory();
            f = new ReflectionFieldSetterFactory();
            o = new ReflectionObjectConstructorFactory();
        }
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

