package org.sql2o.reflection2;

import org.sql2o.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PojoPropertyBuilder {

    private Method getter;
    private Method setter;
    private Field field;
    private String annotatedName;
    private final String name;
    private final Settings settings;

    public PojoPropertyBuilder(String name, Settings settings) {
        this.name = name;
        this.settings = settings;
    }

    public void withGetter(Method getter) {
        this.getter = getter;
    }

    public void withSetter(Method setter) {
        this.setter = setter;
    }

    public void withField(Field field) {
        this.field = field;
    }

    public void withAnnotatedName(String annotatedName) {
        this.annotatedName = annotatedName;
    }

    public PojoProperty build() {
        return new PojoProperty(name, annotatedName, getter, setter, field, settings);
    }
}
