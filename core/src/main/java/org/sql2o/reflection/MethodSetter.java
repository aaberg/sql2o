package org.sql2o.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.sql2o.Sql2oException;

/**
 * used internally to set property values via its setter method.
 */
public class MethodSetter implements  Setter{
    
    private Method method;
    private Class<?> type;

    public MethodSetter(Method method) {
        this.method = method;
        this.method.setAccessible(true);
        type = method.getParameterTypes()[0];
    }

    @Override
    public void setProperty(Object obj, Object value) {
        if (value == null && type.isPrimitive()){
            return; // dont try to set null to a setter to a primitive type.
        }
        try {
            this.method.invoke(obj, value);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("error while calling setter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        } catch (InvocationTargetException e) {
            throw new Sql2oException("error while calling setter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    @Override
    public Class getType() {
        return type;
    }
}
