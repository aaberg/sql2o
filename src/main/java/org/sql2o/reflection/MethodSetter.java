package org.sql2o.reflection;

import org.sql2o.Sql2oException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/15/11
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class MethodSetter implements  Setter{
    
    private Method method;

    public MethodSetter(Method method) {
        this.method = method;
    }

    public void setProperty(Object obj, Object value) {
        try {
            this.method.invoke(obj, value);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("error while calling setter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        } catch (InvocationTargetException e) {
            throw new Sql2oException("error while calling setter method with name " + method.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    public Class getType() {
        return method.getParameterTypes()[0];
    }
}
