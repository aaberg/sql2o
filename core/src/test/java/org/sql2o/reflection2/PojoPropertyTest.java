package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.quirks.NoQuirks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What {@link PojoProperty} hands out of itself: the {@link Field} and the {@link Method} it was built with, so
 * that a caller outside this package can assign through either rather than reflect on the class a second time.
 */
public class PojoPropertyTest {

    // ---------------------------------------------------------------- fixtures

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
        int number;
    }

    // ---------------------------------------------------------------- helpers

    private static Settings plainSettings() {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), true);
    }

    /**
     * A property assembled by hand, the way {@link PojoBuilder} would build one for the named member.
     *
     * @param clazz       the class to read the member off
     * @param name        the property name
     * @param setterName  the setter, or null for a field-only property
     * @param getterName  the getter, or null for a field-only property
     */
    private static PojoProperty propertyFor(Class<?> clazz, Settings settings, String name,
                                           String setterName, String getterName) throws ReflectiveOperationException {
        final Method setter = setterName == null ? null : clazz.getMethod(setterName, String.class);
        final Method getter = getterName == null ? null : clazz.getMethod(getterName);
        final Field field = clazz.getDeclaredField(name);
        return new PojoProperty(name, name, getter, setter, field, settings);
    }

    private static PojoProperty lonelyProperty(Settings settings) {
        return new PojoProperty("lonely", "lonely", null, null, null, settings);
    }

    // ---------------------------------------------------------------- the members behind the property

    /** Both are handed out as they were built, which is all a caller writing through them needs to know. */
    @Test
    public void theFieldAndTheSetterBehindThePropertyAreTheOnesItWasBuiltWith() throws Exception {
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

        assertEquals("value", property.getField().getName());
        assertEquals("setValue", property.getSetter().getName());
    }

    /**
     * A property with a setter as well as a field offers both, and the one {@code SetProperty} writes through is the
     * setter. A caller that picks differently maps the same column onto a different member, which on a setter with a side
     * effect in it is not a difference anybody wants to debug.
     */
    @Test
    public void aSetterIsOfferedEvenWhereAFieldWouldAlsoDo() throws Exception {
        final var pojo = new SetterAndField();
        final var property = propertyFor(SetterAndField.class, plainSettings(), "value", "setValue", "getValue");

        property.SetProperty(pojo, "written", plainSettings().getQuirks());

        assertEquals(1, pojo.setterCalls);
        assertNotNull(property.getSetter());
    }

    /** Null rather than an exception, so that a caller can tell "nothing to write through" from "write through this". */
    @Test
    public void thereIsNoFieldAndNoSetterToOfferWhenThereIsNeither() {
        final PojoProperty property = lonelyProperty(plainSettings());

        assertNull(property.getField());
        assertNull(property.getSetter());
    }

    /** A property built from a field alone still offers its field, which is what a field-only POJO has to write through. */
    @Test
    public void aFieldOnlyPropertyOffersItsFieldAndNoSetter() throws Exception {
        final var property = propertyFor(FieldOnly.class, plainSettings(), "number", null, null);

        assertEquals("number", property.getField().getName());
        assertNull(property.getSetter());
    }
}