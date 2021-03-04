package org.sql2o.reflection;

import java.lang.reflect.Field;
import org.sql2o.Sql2oException;

/**
 * used internally to set property values directly into the field. Only used if no setter method is found.
 */
public class FieldSetter implements Setter{

    private Field field;

    public FieldSetter(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    @Override
    public void setProperty(Object obj, Object value) {
        if (value == null && this.field.getType().isPrimitive()){
            return; // dont try set null to a primitive field
        }

        try {
            this.field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("could not set field " + this.field.getName() + " on class " + obj.getClass().toString(), e);
        }
    }

    @Override
    public Class getType() {
        return field.getType();
    }
}
