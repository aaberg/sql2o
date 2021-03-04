package org.sql2o.reflection;

import java.lang.reflect.Field;
import org.sql2o.Sql2oException;

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

    @Override
    public Object getProperty(Object obj) {
        try {
            return this.field.get(obj);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("could not get field " + this.field.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    @Override
    public Class getType() {
        return field.getType();
    }
}