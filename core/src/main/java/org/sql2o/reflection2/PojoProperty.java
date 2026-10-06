package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PojoProperty {

    private final String name;
    private final String annotatedName;
    private final Method getter;
    private final Method setter;
    private final Field  field;
    // Only read by the deprecated SetProperty(Object, Object). The metadata is shared between all Sql2o
    // instances that map the class, so the quirks captured here belong to whichever instance came first.
    private final Settings settings;

    public PojoProperty(String name, String annotatedName, Method getter, Method setter, Field field, Settings settings) {
        this.name = name;
        this.annotatedName = annotatedName;
        this.getter = getter;
        this.setter = setter;
        this.field = field;
        if (field != null) {
            this.field.setAccessible(true);
        }

        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public String getAnnotatedName() {
        return annotatedName;
    }

    public void SetProperty(Object obj, Object value, Quirks quirks) throws ReflectiveOperationException {
        if (setter != null) {
            try {
                final var propertyType = getSetterType();
                final var convertedValue = quirks.converterOf(propertyType).convert(value);
                if (convertedValue == null && propertyType.isPrimitive()) {
                    return; // don't try to set null to a setter to a primitive type.
                }
                setter.invoke(obj, convertedValue);
            } catch (ConverterException ex) {
                throw new Sql2oException("Error trying to convert value of type " + value.getClass().getName() + " to property " + name + " [" + setter.getName() + "] of type " + setter.getDeclaringClass(), ex);
            }
            return;
        }

        if(field != null) {
            try {
                final var propertyType = field.getType();
                final var convertedValue = quirks.converterOf(propertyType).convert(value);
                if (convertedValue == null && propertyType.isPrimitive()) {
                    return; // don't try to set null to a field to a primitive type.
                }
                field.set(obj, convertedValue);
            } catch (ConverterException ex) {
                throw new Sql2oException("Error trying to convert value of type " + value.getClass().getName() + " to field " + name + " of type " + field.getDeclaringClass(), ex);
            }
            return;
        }

        throw new Sql2oException("No setter or field found for property " + name);
    }

    /**
     * @deprecated the quirks of whichever Sql2o instance happened to build this metadata are used, which is
     * rarely the ones the caller expects, since the metadata of a class is shared between all instances.
     * Use {@link #SetProperty(Object, Object, Quirks)} and pass the quirks explicitly.
     */
    @Deprecated(since = "1.9.0")
    public void SetProperty(Object obj, Object value) throws ReflectiveOperationException {
        SetProperty(obj, value, settings.getQuirks());
    }

    public Class<?> getType() {
        if (setter != null) {
            return setter.getParameters()[0].getType();
        } else if (field != null) {
            return field.getType();
        }

        throw new Sql2oException("Unexpected error. Could not get type of property " + getName());
    }

    /**
     * The field this property writes to, or null when there is none.
     *
     * <p>Public so that a mapping implementation can assign through the field itself rather than reflectively, which
     * is what the bytecode extension does with it. A caller that reproduces the choice
     * {@link #SetProperty(Object, Object, Quirks)} makes has to prefer the setter, and to fall back to this field when
     * there is no setter, or its mapping will not agree with the reflective one on which member gets written.
     *
     * @return the field behind this property, or null if it was built without one
     */
    public Field getField() {
        return field;
    }

    /**
     * The setter this property writes through, or null when there is none.
     *
     * <p>The counterpart of {@link #getField()}, and preferred over it for the same reason: it is what
     * {@link #SetProperty(Object, Object, Quirks)} calls first.
     *
     * @return the setter behind this property, or null if it was built without one
     */
    public Method getSetter() {
        return setter;
    }

    // only used when setting complex types
    public Object getValue(Object obj) throws ReflectiveOperationException {
        if (getter != null) {
            return getter.invoke(obj);
        } else if (field != null) {
            return field.get(obj);
        }

        throw new Sql2oException("No getter or field found for property " + name);
    }

    // only used when setting complex types
    public Object initializeWithNewInstance(Object obj) throws ReflectiveOperationException {
        if (setter != null) {
            final var propertyType = setter.getParameters()[0].getType();
            // create new instance. Assume empty constructor.
            final var instance = propertyType.getDeclaredConstructor().newInstance();
            setter.invoke(obj, instance);
            return instance;
        } else if (field != null) {
            final var propertyType = field.getType();
            // create new instance. Assume empty constructor.
            final var instance = propertyType.getDeclaredConstructor().newInstance();
            field.set(obj, instance);
            return instance;
        }

        throw new Sql2oException("Could not initialize property " + getName() + " no setter or field found.");
    }

    private Class<?> setterType = null;
    private Class<?> getSetterType() {
        if (setterType == null) {
            setterType = setter.getParameters()[0].getType();
        }
        return setterType;
    }
}
