package org.sql2o.reflection;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/15/11
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Setter {

    void setProperty(Object obj, Object value);
    Class getType();
}
