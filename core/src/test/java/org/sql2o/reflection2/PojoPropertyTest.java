package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PojoProperty}, which does the actual assigning of a value to one property.
 *
 * <p>PojoBuilderTest already reaches most of this class, but some paths only exist for a property that was
 * assembled by hand, for instance one without a setter or field at all, or a converter that fails. Those are
 * built directly here.
 */
public class PojoPropertyTest {

    // ---------------------------------------------------------------- fixtures

    public static class BooleanFlag {
        boolean flag;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }

    public static class SetterAndField {
        String value;
        int setterCalls;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            setterCalls++;
        }
    }

    public static class FieldOnly {
        Nested nested;
        int number;
    }

    public static class Nested {
        int val;
    }

    /** Always fails, to reach the conversion error paths of SetProperty. */
    public static class FailingConverter implements Converter<Object> {

        @Override
        public Object convert(Object val) throws ConverterException {
            throw new ConverterException("cannot convert " + val);
        }

        @Override
        public Object toDatabaseParam(Object val) {
            return val;
        }
    }

    /** Records what it was given by converting it to upper case. */
    public static class UpperCaseConverter implements Converter<String> {

        @Override
        public String convert(Object val) {
            return String.valueOf(val).toUpperCase();
        }

        @Override
        public Object toDatabaseParam(String val) {
            return val;
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Map<Class, Converter> converters(Object... classesAndConverters) {
        final var map = new HashMap<Class, Converter>();
        for (int i = 0; i < classesAndConverters.length; i += 2) {
            map.put((Class) classesAndConverters[i], (Converter) classesAndConverters[i + 1]);
        }
        return map;
    }

    private static Settings settingsWith(Map<Class, Converter> converters) {
        return new Settings(new NamingConvention(false, false), new NoQuirks(converters), true);
    }

    private static Settings plainSettings() {
        return settingsWith(converters());
    }

    private static Quirks quirksWith(Map<Class, Converter> converters) {
        return new NoQuirks(converters);
    }

    /** Builds a property for the named members of a fixture, the way PojoMetadata would. */
    private static PojoProperty propertyFor(Class<?> clazz, Settings settings, String fieldName,
                                                    String setterName, String getterName) throws Exception {
        final Field field = fieldName == null ? null : clazz.getDeclaredField(fieldName);
        final Method setter;
        if (setterName == null) {
            setter = null;
        } else {
            final Class<?> type = clazz.getDeclaredField(fieldName).getType();
            setter = clazz.getDeclaredMethod(setterName, type);
        }
        final Method getter = getterName == null ? null : clazz.getDeclaredMethod(getterName);

        return new PojoProperty(fieldName, null, getter, setter, field, settings);
    }

    /** A property with nothing to read from, write to or instantiate. */
    private static PojoProperty lonelyProperty(Settings settings) {
        return new PojoProperty("lonely", null, null, null, null, settings);
    }

    // ---------------------------------------------------------------- SetProperty, setter branch

    @Test
    public void primitiveSetterIsLeftAloneWhenTheConverterYieldsNull() throws Exception {
        final var pojo = new BooleanFlag();
        pojo.flag = true;
        final var settings = plainSettings();
        final var property = propertyFor(BooleanFlag.class, settings, "flag", "setFlag", "isFlag");

        // BooleanConverter hands null straight back for a primitive property, unlike NumberConverter which
        // returns the primitive default, so this guard is reached for booleans.
        property.SetProperty(pojo, null, settings.getQuirks());

        assertTrue(pojo.flag);
    }

    @Test
    public void converterFailureOnASetterIsReported() throws Exception {
        final var pojo = new SetterAndField();
        final var failing = converters(String.class, new FailingConverter());
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

        final var ex = assertThrows(Sql2oException.class, () ->
                property.SetProperty(pojo, "text", quirksWith(failing)));

        assertEquals("Error trying to convert value of type java.lang.String to property value [setValue] of type "
                + SetterAndField.class, ex.getMessage());
    }

    @Test
    public void converterFailureOnAFieldIsReported() throws Exception {
        final var pojo = new FieldOnly();
        final var failing = converters(int.class, new FailingConverter());
        final var property = propertyFor(FieldOnly.class, plainSettings(), "number", null, null);

        final var ex = assertThrows(Sql2oException.class, () ->
                property.SetProperty(pojo, 1, quirksWith(failing)));

        assertEquals("Error trying to convert value of type java.lang.Integer to field number of type "
                + FieldOnly.class, ex.getMessage());
    }

    @Test
    public void theDeprecatedOverloadUsesTheQuirksOfTheMetadata() throws Exception {
        final var pojo = new SetterAndField();
        final var settings = settingsWith(converters(String.class, new UpperCaseConverter()));
        final var property = propertyFor(SetterAndField.class, settings, "value", "setValue", "getValue");

        property.SetProperty(pojo, "text");

        assertEquals("TEXT", pojo.value);
        assertEquals(1, pojo.setterCalls);
    }

    @Test
    public void aPropertyWithoutASetterOrFieldIsReportedWhenSettingAValue() {
        final var settings = plainSettings();
        final PojoProperty property = lonelyProperty(settings);

        final var ex = assertThrows(Sql2oException.class, () ->
                property.SetProperty(new Object(), "x", settings.getQuirks()));

        assertEquals("No setter or field found for property lonely", ex.getMessage());
    }

    @Test
    public void theSetterTakesPrecedenceOverTheField() throws Exception {
        final var pojo = new SetterAndField();
        final var settings = plainSettings();
        final var property = propertyFor(SetterAndField.class, settings, "value", "setValue", "getValue");

        property.SetProperty(pojo, "written", settings.getQuirks());

        assertEquals("written", pojo.value);
        assertEquals(1, pojo.setterCalls);
        assertNull(property.getAnnotatedName());
        assertEquals("value", property.getName());
    }

    // ---------------------------------------------------------------- getType

    @Test
    public void getTypePrefersTheSetterParameterType() throws Exception {
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

        assertEquals(String.class, property.getType());
    }

    @Test
    public void getTypeFallsBackToTheFieldType() throws Exception {
        final var property = propertyFor(FieldOnly.class, plainSettings(), "number", null, null);

        assertEquals(int.class, property.getType());
    }

    @Test
    public void getTypeWithoutSetterOrFieldIsReported() {
        final PojoProperty property = lonelyProperty(plainSettings());

        final var ex = assertThrows(Sql2oException.class, property::getType);

        assertEquals("Unexpected error. Could not get type of property lonely", ex.getMessage());
    }

    // ---------------------------------------------------------------- getValue

    @Test
    public void getValueReadsThroughTheGetter() throws Exception {
        final var pojo = new SetterAndField();
        pojo.value = "read";
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

        assertEquals("read", property.getValue(pojo));
    }

    @Test
    public void getValueFallsBackToTheField() throws Exception {
        final var pojo = new FieldOnly();
        final var existing = new Nested();
        pojo.nested = existing;
        final var property = propertyFor(FieldOnly.class, plainSettings(), "nested", null, null);

        assertSame(existing, property.getValue(pojo));
    }

    @Test
    public void getValueWithoutGetterOrFieldIsReported() {
        final PojoProperty property = lonelyProperty(plainSettings());

        final var ex = assertThrows(Sql2oException.class, () -> property.getValue(new Object()));

        assertEquals("No getter or field found for property lonely", ex.getMessage());
    }

    // ---------------------------------------------------------------- initializeWithNewInstance

    @Test
    public void initializeWithNewInstanceUsesTheSetterParameterTypeAndAssignsIt() throws Exception {
        final var pojo = new SetterAndField();
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

final var instance = property.initializeWithNewInstance(pojo);

        // the instance comes from the empty constructor of the parameter type, and the setter is what puts
        // it into the object, so it is invoked exactly once
        assertEquals("", instance);
        assertEquals("", pojo.value);
        assertEquals(1, pojo.setterCalls);
    }

    @Test
    public void initializeWithNewInstanceUsesTheFieldTypeAndAssignsIt() throws Exception {
        final var pojo = new FieldOnly();
        final var property = propertyFor(FieldOnly.class, plainSettings(), "nested", null, null);

        final var instance = property.initializeWithNewInstance(pojo);

        assertNotNull(instance);
        assertSame(instance, pojo.nested);
    }

    @Test
    public void initializeWithNewInstanceWithoutSetterOrFieldIsReported() {
        final PojoProperty property = lonelyProperty(plainSettings());

        final var ex = assertThrows(Sql2oException.class, () -> property.initializeWithNewInstance(new Object()));

        assertEquals("Could not initialize property lonely no setter or field found.", ex.getMessage());
    }
}