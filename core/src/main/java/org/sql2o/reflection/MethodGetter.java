package org.sql2o.reflection;

import org.sql2o.Sql2oException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * used internally to get property values via its getter method.
 *
 * @author mdelapenya
 */
public class MethodGetter implements Getter {
    
    private Method method;
    private Class<?> type;

    public MethodGetter(Method method) {
        this.method = method;
        this.method.setAccessible(true);
        type = method.getReturnType();
    }

    public Object getProperty(Object obj) {
        try {
            return this.method.invoke(obj);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("error while calling getter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        } catch (InvocationTargetException e) {
            throw new Sql2oException("error while calling getter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    public Class getType() {
        return type;
    }
}
