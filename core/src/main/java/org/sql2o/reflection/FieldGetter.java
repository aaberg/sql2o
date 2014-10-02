package org.sql2o.reflection;

import org.sql2o.Sql2oException;

import java.lang.reflect.Field;

/**
 * used internally to get property values directly from the field. Only used if no getter method is found.
 *
 * @author mdelapenya
 */
public class FieldGetter implements Getter {

    private Field field;

    public FieldGetter(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    public Object getProperty(Object obj) {
        try {
            return this.field.get(obj);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("could not get field " + this.field.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    public Class getType() {
        return field.getType();
    }
}