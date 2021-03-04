package org.sql2o.reflection;

import java.lang.reflect.Method;

/**
 * @author mdelapenya
 */
public class ReflectionMethodGetterFactory implements MethodGetterFactory {
    @Override
    public Getter newGetter(Method method) {
        return new MethodGetter(method);
    }
}