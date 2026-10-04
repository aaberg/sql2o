package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;

import javax.persistence.Column;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PojoBuilder}, which maps the values of one row onto a single object.
 *
 * <p>The builder is driven directly instead of going through a database: PojoMetadata is built here rather
 * than taken from the shared cache in ObjectBuildableFactory, so every test is independent of the execution
 * order and nothing leaks between them.
 */
public class PojoBuilderTest {

    // ---------------------------------------------------------------- fixtures

    public static class FieldPojo {
        int id;
        String name;
    }

    /** Has a public field and a setter for the same property, to show which one is used. */
    public static class SetterPojo {
        int val;
        int setterVal;

        public void setVal(int val) {
            this.setterVal = val;
        }

        public int getSetterVal() {
            return setterVal;
        }
    }

    /** The field is private and there is a getter but no setter. */
    public static class PrivateFieldPojo {
        private int id;

        public int getId() {
            return id;
        }
    }

    public static class GetterOnlyPojo {
        public int getComputed() {
            return 42;
        }
    }

    public static class WithStatic {
        static int counter;
        int id;
    }

    public static class AnnotatedPojo {
        @Column(name = "user_id")
        int userId;
    }

    public static class Primitives {
        int number;
        String text;
    }

    public static class DeepLevel {
        int val;
    }

    public static class Nested {
        int val;
        DeepLevel deeper;
    }

    public static class Parent {
        Nested nested;
        int nestedSetterCalls;

        public Nested getNested() {
            return nested;
        }

        public void setNested(Nested nested) {
            this.nested = nested;
            this.nestedSetterCalls++;
        }
    }

    public static class Wrapper {
        final String value;

        Wrapper(String value) {
            this.value = value;
        }
    }

    public static class WrappedPojo {
        Wrapper wrapped;
    }

    public static class UpperCaseConverter implements Converter<Wrapper> {

        @Override
        public Wrapper convert(Object val) throws ConverterException {
            return new Wrapper(String.valueOf(val).toUpperCase());
        }

        @Override
        public Object toDatabaseParam(Wrapper val) {
            return val.value;
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Settings settings() {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), true);
    }

    private static Settings lenientSettings() {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), false);
    }

    private static <T> PojoBuilder<T> builder(Class<T> clazz, Settings settings, Map<String, String> columnMappings) {
        try {
            return new PojoBuilder<>(settings, new PojoMetadata<>(clazz, settings), columnMappings);
        } catch (ReflectiveOperationException e) {
            throw new Sql2oException("Could not create a builder for " + clazz, e);
        }
    }

    private static <T> PojoBuilder<T> builder(Class<T> clazz) {
        return builder(clazz, settings(), Map.of());
    }

    // ---------------------------------------------------------------- flat columns

    @Test
    public void publicFieldsAreAssigned() throws ReflectiveOperationException {
        final var builder = builder(FieldPojo.class);

        builder.withValue("id", 7);
        builder.withValue("name", "seven");

        final var pojo = builder.build();
        assertEquals(7, pojo.id);
        assertEquals("seven", pojo.name);
    }

    @Test
    public void theLastValueOfAColumnWins() throws ReflectiveOperationException {
        final var builder = builder(FieldPojo.class);

        builder.withValue("name", "first");
        builder.withValue("name", "second");

        assertEquals("second", builder.build().name);
    }

    @Test
    public void setterIsUsedBeforeTheField() throws ReflectiveOperationException {
        final var builder = builder(SetterPojo.class);

        builder.withValue("val", 5);

        final var pojo = builder.build();
        assertEquals(5, pojo.setterVal);
        assertEquals(0, pojo.val);
    }

    @Test
    public void privateFieldIsAssignedWithoutASetter() throws ReflectiveOperationException {
        final var builder = builder(PrivateFieldPojo.class);

        builder.withValue("id", 3);

        assertEquals(3, builder.build().getId());
    }

    @Test
    public void propertyWithoutSetterOrFieldIsReported() {
        final var builder = builder(GetterOnlyPojo.class);

        final var ex = assertThrows(Sql2oException.class, () -> builder.withValue("computed", 1));

        assertEquals("No setter or field found for property computed", ex.getMessage());
    }

    @Test
    public void staticFieldsAreNotMapped() throws ReflectiveOperationException {
        final var builder = builder(WithStatic.class);

        assertThrows(Sql2oException.class, () -> builder.withValue("counter", 1));

        builder.withValue("id", 2);
        assertEquals(2, builder.build().id);
    }

    @Test
    public void columnMappingRedirectsToAnotherProperty() throws ReflectiveOperationException {
        final var builder = builder(FieldPojo.class, settings(), Map.of("caption", "name"));

        builder.withValue("caption", "mapped");

        assertEquals("mapped", builder.build().name);
    }

    @Test
    public void columnAnnotationAddsAnAliasForTheField() throws ReflectiveOperationException {
        final var builder = builder(AnnotatedPojo.class);

        builder.withValue("user_id", 9);

        assertEquals(9, builder.build().userId);
    }

    @Test
    public void valueIsConvertedWithTheQuirksOfTheInstance() throws ReflectiveOperationException {
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(Wrapper.class, new UpperCaseConverter());
        final var withConverter = new Settings(new NamingConvention(false, false), new NoQuirks(converters), true);

        final var builder = builder(WrappedPojo.class, withConverter, Map.of());
        builder.withValue("wrapped", "abc");

        assertEquals("ABC", builder.build().wrapped.value);
    }

    @Test
    public void nullBecomesThePrimitiveDefaultAndNullForObjects() throws ReflectiveOperationException {
        final var builder = builder(Primitives.class);

        builder.withValue("number", null);
        builder.withValue("text", null);

        // NumberConverter turns null into the primitive default for primitive properties, so the guard in
        // PojoProperty that skips null for primitives never applies to them.
        final var pojo = builder.build();
        assertEquals(0, pojo.number);
        assertNull(pojo.text);
    }

    // ---------------------------------------------------------------- dotted columns

    @Test
    public void dottedColumnCreatesAndFillsTheNestedObject() throws ReflectiveOperationException {
        final var builder = builder(Parent.class);

        builder.withValue("nested.val", 42);

        final var pojo = builder.build();
        assertNotNull(pojo.nested);
        assertEquals(42, pojo.nested.val);
    }

    @Test
    public void dottedColumnInvokesTheNestedSetterExactlyOnce() throws ReflectiveOperationException {
        final var builder = builder(Parent.class);

        builder.withValue("nested.val", 42);

        assertEquals(1, builder.build().nestedSetterCalls);
    }

    @Test
    public void existingNestedObjectIsReusedAndNotAssignedAgain() throws ReflectiveOperationException {
        final var builder = builder(Parent.class);
        final var existing = new Nested();
        builder.withValue("nested", existing);

        builder.withValue("nested.val", 1);
        builder.withValue("nested.val", 2);

        final var pojo = builder.build();
        assertSame(existing, pojo.nested);
        assertEquals(2, pojo.nested.val);
        // one call from the explicit assignment above, none from the two dotted columns
        assertEquals(1, pojo.nestedSetterCalls);
    }

    @Test
    public void everyIntermediateObjectOfADottedPathIsCreated() throws ReflectiveOperationException {
        final var builder = builder(Parent.class);

        builder.withValue("nested.deeper.val", 3);

        final var pojo = builder.build();
        assertNotNull(pojo.nested);
        assertNotNull(pojo.nested.deeper);
        assertEquals(3, pojo.nested.deeper.val);
        assertEquals(1, pojo.nestedSetterCalls);
    }

    @Test
    public void unknownPrefixOfADottedColumnIsReportedWithTheFullPath() {
        final var builder = builder(Parent.class);

        final var ex = assertThrows(Sql2oException.class, () -> builder.withValue("missing.val", 1));

        assertEquals("Could not map missing.val to any property.", ex.getMessage());
    }

    @Test
    public void unknownLeafOfADottedColumnIsReportedWithTheRemainingPath() {
        final var builder = builder(Parent.class);

        final var ex = assertThrows(Sql2oException.class, () -> builder.withValue("nested.missing", 1));

        assertEquals("Could not map missing to any property.", ex.getMessage());
    }

    @Test
    public void dottedColumnIsIgnoredWhenMappingErrorsAreOff() throws ReflectiveOperationException {
        final var builder = builder(Parent.class, lenientSettings(), Map.of());

        builder.withValue("missing.val", 1);

        assertNull(builder.build().nested);
    }

    @Test
    public void unknownColumnIsReported() {
        final var builder = builder(FieldPojo.class);

        final var ex = assertThrows(Sql2oException.class, () -> builder.withValue("nope", 1));

        assertEquals("Could not map nope to any property.", ex.getMessage());
    }

    @Test
    public void unknownColumnIsIgnoredWhenMappingErrorsAreOff() throws ReflectiveOperationException {
        final var builder = builder(FieldPojo.class, lenientSettings(), Map.of());

        builder.withValue("nope", 1);
        builder.withValue("id", 4);

        assertEquals(4, builder.build().id);
    }

    // ---------------------------------------------------------------- construction

    @Test
    public void buildReturnsTheInstanceTheBuilderWasGiven() throws ReflectiveOperationException {
        final var existing = new FieldPojo();

        final PojoBuilder<FieldPojo> builder;
        try {
            builder = new PojoBuilder<>(settings(), new PojoMetadata<>(FieldPojo.class, settings()), Map.of(), existing);
        } catch (ReflectiveOperationException e) {
            throw new Sql2oException(e);
        }

        assertSame(existing, builder.build());
    }
}