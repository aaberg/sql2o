package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.quirks.NoQuirks;

import javax.persistence.Column;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link PojoMetadata}, which decides which property a column belongs to.
 *
 * <p>PojoMetadata is built directly here, bypassing the cache in ObjectBuildableFactory, so the tests do not
 * depend on each other.
 */
public class PojoMetadataTest {

    // ---------------------------------------------------------------- fixtures

    /** A boolean getter is registered under its name without the "is" prefix. */
    public static class BooleanGetter {
        boolean active;

        public boolean isActive() {
            return active;
        }
    }

    /** Members that look like accessors but are not usable as properties. */
    public static class NotAccessors {
        int size;

public int getWithArgument(int ignored) {
            return ignored;
        }

        public boolean isWithArgument(int ignored) {
            return ignored > 0;
        }

        public void setWithoutArgument() {
            // no parameter, so not a setter
        }

        public int computeSomething() {
            return size;
        }

        public int size() {
            return size;
        }
    }

    public static class WithSetterAndField {
        int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /** A field carrying the annotation. */
    public static class AnnotatedField {
        @Column(name = "user_id")
        int userId;
    }

    /**
     * The method branch of the annotation lookup probes the map with the method name, not with the property
     * name, so a method only contributes a column name if it is named exactly like the property. Both members
     * below are called "userid", which the derived property name of the field happens to be.
     */
    public static class MethodNamedLikeItsProperty {
        @Column(name = "from_field")
        int userId;

        public int userId() {
            return userId;
        }
    }


    public static class BaseWithLongSetter {
        long value;

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    /** Declares a second setValue with a narrower parameter type, so that the two are distinguishable. */
    public static class ChildWithIntSetter extends BaseWithLongSetter {
        public void setValue(int value) {
            super.setValue(value);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Settings settings() {
        return new Settings(new NamingConvention(false, false), new NoQuirks(), true);
    }

    private static PojoMetadata<?> metadataFor(Class<?> clazz) {
        try {
            return new PojoMetadata<>(clazz, settings());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- properties

    @Test
    public void booleanGetterIsRegisteredWithoutTheIsPrefix() {
        final var metadata = metadataFor(BooleanGetter.class);

        assertNotNull(metadata.getPojoProperty("active", Map.of()));
    }

@Test
    public void getterWithAParameterIsNotAProperty() {
        final var metadata = metadataFor(NotAccessors.class);

        assertNull(metadata.getPojoProperty("withargument", Map.of()));
    }

    @Test
    public void booleanGetterWithAParameterIsNotAProperty() {
        final var metadata = metadataFor(NotAccessors.class);

        assertNull(metadata.getPojoProperty("iswithargument", Map.of()));
    }

    @Test
    public void setterWithoutAParameterIsNotAProperty() {
        final var metadata = metadataFor(NotAccessors.class);

        assertNull(metadata.getPojoProperty("withoutargument", Map.of()));
    }

    @Test
    public void methodThatIsNotAnAccessorIsNotAProperty() {
        final var metadata = metadataFor(NotAccessors.class);

        assertNull(metadata.getPojoProperty("computesomething", Map.of()));
    }

    @Test
    public void fieldAndAccessorAreOneProperty() {
        final var metadata = metadataFor(WithSetterAndField.class);

        final var property = metadata.getPojoProperty("value", Map.of());

        assertNotNull(property);
        assertEquals("value", property.getName());
        assertEquals(int.class, property.getType());
    }

    @Test
    public void inheritedPropertyIsVisible() {
        final var metadata = metadataFor(ChildWithIntSetter.class);

        assertNotNull(metadata.getPojoProperty("value", Map.of()));
    }

    /**
     * Documents current behaviour, not the intent: the recursion walks the subclass first and the superclass
     * last, and PojoPropertyBuilder always overwrites, so the superclass setter replaces the overriding one.
     * The two setters take different types, which is what makes the loss observable through getType().
     */
    @Test
    public void superclassSetterReplacesTheOverridingSubclassSetter() {
        final var metadata = metadataFor(ChildWithIntSetter.class);

        final var property = metadata.getPojoProperty("value", Map.of());

        assertNotNull(property);
        assertEquals(long.class, property.getType());
    }

    // ---------------------------------------------------------------- column mappings

    @Test
    public void columnMappingRedirectsToTheMappedProperty() {
        final var metadata = metadataFor(WithSetterAndField.class);

        final var mapped = metadata.getPojoProperty("caption", Map.of("caption", "value"));

        assertNotNull(mapped);
        assertEquals("value", mapped.getName());
    }

    @Test
    public void columnMappingToAnUnknownPropertyFallsBackToTheColumnName() {
        final var metadata = metadataFor(WithSetterAndField.class);

        assertNull(metadata.getPojoProperty("caption", Map.of("caption", "nosuchproperty")));
    }

    @Test
    public void columnWithoutAMappingIsLookedUpByItsOwnName() {
        final var metadata = metadataFor(WithSetterAndField.class);

        assertNotNull(metadata.getPojoProperty("value", Map.of("other", "value")));
    }

    // ---------------------------------------------------------------- annotations

    @Test
    public void columnAnnotationOnAFieldAddsAnAlias() {
        final var metadata = metadataFor(AnnotatedField.class);

        assertNotNull(metadata.getPojoProperty("user_id", Map.of()));
    }

    /**
     * Documents that the method branch of the annotation lookup never contributes a column name: when the
     * methods are walked, the map holds only properties created by accessors seen so far, and fields are only
     * walked afterwards, so a method named like a property finds nothing. Field annotations therefore stand.
     */
    @Test
    public void methodNamedLikeItsPropertyDoesNotOverrideTheFieldAnnotation() {
        final var metadata = metadataFor(MethodNamedLikeItsProperty.class);

        assertNotNull(metadata.getPojoProperty("from_field", Map.of()));
        assertNotNull(metadata.getPojoProperty("userid", Map.of()));
    }

    @Test
    public void constructorIsTheNoArgOne() throws Exception {
        final var metadata = metadataFor(WithSetterAndField.class);

        assertEquals(WithSetterAndField.class.getDeclaredConstructor(), metadata.getConstructor());
    }
}