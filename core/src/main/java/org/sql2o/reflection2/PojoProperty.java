package org.sql2o.reflection2;

import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class PojoProperty {

    private final String name;
    private final String annotatedName;
    private final Method getter;
    private final Method setter;
    private final Field  field;
    // Only read by the deprecated SetProperty(Object, Object). The metadata is shared between all Sql2o
    // instances that map the class, so the quirks captured here belong to whichever instance came first.
    private final Settings settings;
    // The same assignment through handles, computed once rather than reflected on every row. Either is null when
    // the member cannot be reached that way, and the assignment below falls back to reflection.
    private final MethodHandle setterHandle;
    private final VarHandle fieldHandle;

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
        this.setterHandle = unreflectSetter(setter);
        this.fieldHandle = unreflectField(field);
    }

    /**
     * The setter as a handle, or null when it cannot be reached that way — a private member in a package this
     * lookup cannot open, for instance — in which case the assignment falls back to reflection and fails there
     * exactly as before.
     */
    private static MethodHandle unreflectSetter(Method setter) {
        if (setter == null) {
            return null;
        }
        try {
            return MethodHandles.privateLookupIn(setter.getDeclaringClass(), MethodHandles.lookup())
                    .unreflect(setter);
        } catch (IllegalAccessException | SecurityException e) {
            return null;
        }
    }

    /**
     * The field as a handle, or null with a fallback to reflection. Final fields are left to reflection on purpose:
     * a handle to one is read-only and would refuse the write the reflective path performs.
     */
    private static VarHandle unreflectField(Field field) {
        if (field == null || Modifier.isFinal(field.getModifiers())) {
            return null;
        }
        try {
            return MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
                    .unreflectVarHandle(field);
        } catch (IllegalAccessException | SecurityException e) {
            return null;
        }
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
                setConverted(obj, convertedValue);
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
                setConverted(obj, convertedValue);
            } catch (ConverterException ex) {
                throw new Sql2oException("Error trying to convert value of type " + value.getClass().getName() + " to field " + name + " of type " + field.getDeclaringClass(), ex);
            }
            return;
        }

        throw new Sql2oException("No setter or field found for property " + name);
    }

    /**
     * Assigns an already converted value, which is the second half of {@link #SetProperty(Object, Object, Quirks)}.
     *
     * <p>The caller converts with the quirks beforehand, so that the conversion — and in particular which converter
     * runs and when its failure is reported — stays in one place. What is here is only the null guard for primitives
     * and the assignment itself, exactly as {@code SetProperty} does it.
     *
     * <p>The property has to have a setter or a field; without either there is nothing to assign through. Callers
     * that resolve properties beforehand report that case the way {@code SetProperty} does rather than calling this.
     */
    public void setConverted(Object target, Object convertedValue) throws ReflectiveOperationException {
        if (convertedValue == null && getType().isPrimitive()) {
            return; // don't try to set null to a primitive type.
        }
        // The handles run only when the value is assignable without conversion. A mismatch goes through reflection,
        // which reports it exactly as before: a handle would fail it with a WrongMethodTypeException or with a cast
        // the message of which names the wrong types, while reflection says what could not be set where.
        if (setter != null) {
            if (setterHandle != null && directlyAssignable(getSetterType(), convertedValue)) {
                try {
                    setterHandle.invoke(target, convertedValue);
                } catch (ReflectiveOperationException | RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    // A checked exception out of the setter itself, which reflection would have wrapped.
                    throw new InvocationTargetException(e);
                }
                return;
            }
            setter.invoke(target, convertedValue);
            return;
        }
        if (fieldHandle != null && directlyAssignable(field.getType(), convertedValue)) {
            fieldHandle.set(target, convertedValue);
            return;
        }
        field.set(target, convertedValue);
    }

    /**
     * Whether the value goes into the member without conversion: a reference goes into a reference it is an instance
     * of, and only a wrapper goes into a primitive, which both the handle and reflection unbox. Anything else takes
     * the reflective path, which is also the one that reports it.
     */
    private static boolean directlyAssignable(Class<?> target, Object value) {
        if (value == null) {
            return true;
        }
        if (!target.isPrimitive()) {
            return target.isInstance(value);
        }
        if (target == boolean.class) {
            return value instanceof Boolean;
        }
        if (target == byte.class) {
            return value instanceof Byte;
        }
        if (target == short.class) {
            return value instanceof Short;
        }
        if (target == int.class) {
            return value instanceof Integer;
        }
        if (target == long.class) {
            return value instanceof Long;
        }
        if (target == char.class) {
            return value instanceof Character;
        }
        if (target == float.class) {
            return value instanceof Float;
        }
        return value instanceof Double;
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
            return getSetterType();
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
