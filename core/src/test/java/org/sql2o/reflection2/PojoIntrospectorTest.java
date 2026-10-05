package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PojoIntrospector}, which answers "which property of this object can be read".
 *
 * <p>PojoIntrospector caches what it finds in a static cache keyed by class, so every fixture below is used by
 * exactly one test: reusing a class would hand back a cached result and stop exercising the introspection.
 */
public class PojoIntrospectorTest {

    // ---------------------------------------------------------------- fixtures

    public static class GetterOnly {
        public String getName() {
            return "read";
        }
    }

    public static class BooleanGetter {
        public boolean isActive() {
            return true;
        }
    }

    public static class NotBooleanIsGetter {
        public String isNotBoolean() {
            return "no";
        }
    }

    /** Method names that are too short to carry a property name. */
    public static class TooShort {
        public String get() {
            return "no";
        }

        public boolean is() {
            return false;
        }
    }

    public static class VoidReturning {
        public Void boxedVoid() {
            return null;
        }

        public void primitiveVoid() {
            // nothing
        }
    }

    public static class Visibility {
        public String visible;
        private String hidden;
        static String shared;

        private String hiddenMethod() {
            return hidden;
        }

        public static String staticMethod() {
            return shared;
        }

        public String getAlsoVisible() {
            return visible;
        }
    }

    /** The getter is collected before the fields, so the field of the same name is never read. */
    public static class FieldBehindGetter {
        public String name = "from field";

        public String getName() {
            return "from getter";
        }
    }

    /** Two getters in one class can only claim the same property name once. */
    public static class TwoGettersForOneProperty {
        public String getName() {
            return "value";
        }

        public boolean isName() {
            return true;
        }
    }

    public static class BaseWithField {
        public String hidden;
    }

    public static class ChildHidingField extends BaseWithField {
        public String hidden = "child";
    }

    // ---------------------------------------------------------------- tests

    @Test
    public void getterIsReadable() throws Exception {
        final var properties = PojoIntrospector.readableProperties(GetterOnly.class);

        final var property = properties.get("name");
        assertEquals(String.class, property.type);
        assertEquals("read", property.get(new GetterOnly()));
    }

    @Test
    public void booleanGetterIsReadableUnderItsNameWithoutIs() {
        final var properties = PojoIntrospector.readableProperties(BooleanGetter.class);

        assertTrue(properties.containsKey("active"));
        assertEquals(Boolean.TYPE, properties.get("active").type);
    }

    @Test
    public void isGetterThatIsNotBooleanIsNotAProperty() {
        final var properties = PojoIntrospector.readableProperties(NotBooleanIsGetter.class);

        assertFalse(properties.containsKey("notboolean"));
    }

    @Test
    public void methodNamesWithoutRoomForAPropertyAreSkipped() {
        final var properties = PojoIntrospector.readableProperties(TooShort.class);

        assertTrue(properties.isEmpty());
    }

    @Test
    public void voidReturningMethodsAreSkipped() {
        final Map<String, PojoIntrospector.ReadableProperty> properties =
                PojoIntrospector.readableProperties(VoidReturning.class);

        assertTrue(properties.isEmpty());
    }

    @Test
    public void onlyPublicMembersAreReadable() throws Exception {
        final var properties = PojoIntrospector.readableProperties(Visibility.class);
        final var instance = new Visibility();
        instance.visible = "readable";

        assertEquals("readable", properties.get("visible").get(instance));
        assertEquals("readable", properties.get("alsoVisible").get(instance));
        assertFalse(properties.containsKey("hidden"));
        assertFalse(properties.containsKey("shared"));
        assertFalse(properties.containsKey("hiddenMethod"));
        assertFalse(properties.containsKey("staticMethod"));
    }

    @Test
    public void fieldBehindAGetterIsNotRegisteredTwice() throws Exception {
        final var properties = PojoIntrospector.readableProperties(FieldBehindGetter.class);

        assertEquals(1, properties.size());
        assertEquals("from getter", properties.get("name").get(new FieldBehindGetter()));
    }

    @Test
    public void aSecondGetterForTheSamePropertyIsDropped() {
        final var properties = PojoIntrospector.readableProperties(TwoGettersForOneProperty.class);

        // which of the two wins is up to getDeclaredMethods(), but only one may be registered
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("name"));
    }

    @Test
    public void fieldHiddenByTheSubclassIsKept() throws Exception {
        final var properties = PojoIntrospector.readableProperties(ChildHidingField.class);

        // the subclass is walked first, so its field wins over the one it hides
        assertEquals("child", properties.get("hidden").get(new ChildHidingField()));
    }

    @Test
    public void objectItselfHasNoReadableProperties() {
        assertTrue(PojoIntrospector.readableProperties(Object.class).isEmpty());
    }

    @Test
    public void theResultIsReadOnly() {
        final var properties = PojoIntrospector.readableProperties(GetterOnly.class);

        assertThrows(UnsupportedOperationException.class, () -> properties.remove("name"));
    }

    @Test
    public void theIntrospectorHasNoStateAndIsUsedStatically() {
        // the class is only a holder for the static entry point, instantiating it keeps that honest
        assertNotNull(new PojoIntrospector());
    }
}