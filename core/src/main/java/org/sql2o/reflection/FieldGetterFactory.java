package org.sql2o.reflection;

import java.lang.reflect.Field;

/**
 * @author mdelapenya
 */
public interface FieldGetterFactory {
    Getter newGetter(Field field);
}