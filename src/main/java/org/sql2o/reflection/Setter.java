package org.sql2o.reflection;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/15/11
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Setter {
    
//    boolean initialize(Class objClass, String propertyName, Class propertyClass);
//
    void setProperty(Object obj, Object value);
    Class getType();
}
