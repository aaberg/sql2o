package org.sql2o.reflection;

/**
 * The Getter interface is used by sql2o to get property values when doing automatic column to property mapping
 *
 * @author mdelapenya
 */
public interface Getter {

	Object getProperty(Object obj);
    Class getType();
}
