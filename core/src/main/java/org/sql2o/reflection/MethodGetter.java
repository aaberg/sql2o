package org.sql2o.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.sql2o.Sql2oException;

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

    @Override
    public Object getProperty(Object obj) {
        try {
            return this.method.invoke(obj);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("error while calling getter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        } catch (InvocationTargetException e) {
            throw new Sql2oException("error while calling getter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    @Override
    public Class getType() {
        return type;
    }
}
