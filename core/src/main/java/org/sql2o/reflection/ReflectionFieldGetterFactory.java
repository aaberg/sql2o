package org.sql2o.reflection;

import java.lang.reflect.Field;

/**
 * @author mdelapenya
 */
public class ReflectionFieldGetterFactory implements FieldGetterFactory {
    public Getter newGetter(Field field) {
        return new FieldGetter(field);
    }
}