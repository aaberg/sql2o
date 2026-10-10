package org.sql2o.reflection2;

import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.NoQuirks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for how a column name and a property name are matched against each other.
 *
 * <p>Both sides go through {@link NamingConvention#deriveName}: the column name in {@link PojoBuilder#withValue} and
 * the getter or field name when the metadata is built. The consequence is worth knowing before relying on the case
 * sensitive setting:
 *
 * <ul>
 *   <li>a property name is derived from a getter by dropping "get", which leaves a leading capital, and
 *       {@link org.sql2o.tools.SnakeToCamelCase} lowers every character that is not behind an underscore, so the
 *       property ends up as "mycolumn";</li>
 *   <li>a column keeps whatever case the database used, so an upper case column stays "MY_COLUMN" or becomes
 *       "myColumn".</li>
 * </ul>
 *
 * <p>Neither of those equals the other, so an upper case column only reaches a property when matching is case
 * insensitive. A column that is already lower case matches case sensitively.
 */
public class ColumnToPropertyMatchingTest {

    public static class camelCase {

        private int myColumn;

        public int getMyColumn() {
            return myColumn;
        }

        public void setMyColumn(int myColumn) {
            this.myColumn = myColumn;
        }
    }

    public static class snake_case {

        private int my_column;

        public int getMy_column() {
            return my_column;
        }

        public void setMy_column(int my_column) {
            this.my_column = my_column;
        }
    }

    private static PojoBuilder<?> builderFor(Class<?> clazz, boolean caseSensitive, boolean autoDerive)
            throws ReflectiveOperationException {
        final Settings settings = new Settings(new NamingConvention(caseSensitive, autoDerive), new NoQuirks(), true);
        return new PojoBuilder<>(settings, new PojoMetadata<>(clazz, settings), Map.of());
    }

    private static Object mapped(Class<?> clazz, boolean caseSensitive, boolean autoDerive, String columnName, int value)
            throws ReflectiveOperationException {
        final PojoBuilder<?> builder = builderFor(clazz, caseSensitive, autoDerive);
        builder.withValue(columnName, value);
        return builder.build();
    }

    /** Reads the field the column was supposed to reach, whatever it is called. */
    private static int fieldOf(Object target, String fieldName) {
        try {
            final var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Deriving the names and ignoring the case is the combination an upper case column needs. */
    @Test
    public void anUpperCaseColumnReachesACamelCasePropertyWhenTheCaseIsIgnored() throws Exception {
        assertEquals(1, fieldOf(mapped(camelCase.class, false, true, "MY_COLUMN", 1), "myColumn"));
        assertEquals(2, fieldOf(mapped(camelCase.class, false, true, "my_column", 2), "myColumn"));
    }

    @Test
    public void anUpperCaseColumnReachesASnakeCasePropertyWhenTheCaseIsIgnored() throws Exception {
        assertEquals(3, fieldOf(mapped(snake_case.class, false, false, "MY_COLUMN", 3), "my_column"));
    }

    /**
     * Case sensitively the two names do not line up: the property is keyed "mycolumn" while the column is keyed
     * "myColumn", so the value is reported as unmappable.
     */
    @Test
    public void anUpperCaseColumnCannotReachACamelCasePropertyCaseSensitively() {
        assertThrows(Sql2oException.class, () -> mapped(camelCase.class, true, true, "MY_COLUMN", 1));
        assertThrows(Sql2oException.class, () -> mapped(camelCase.class, true, false, "MY_COLUMN", 1));
    }

    /** A column that is spelled the way the property is does match without ignoring the case. */
    @Test
    public void aLowerCaseColumnMatchesCaseSensitively() throws Exception {
        assertEquals(4, fieldOf(mapped(camelCase.class, false, true, "my_column", 4), "myColumn"));

        final Settings strict = new Settings(new NamingConvention(true, true), new NoQuirks(), true);
        final PojoBuilder<camelCase> builder =
                new PojoBuilder<>(strict, new PojoMetadata<>(camelCase.class, strict), Map.of());
        builder.withValue("mycolumn", 5);

        assertEquals(5, builder.build().getMyColumn());
    }

    @Test
    public void aColumnWithoutAMatchingPropertyIsReported() {
        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> mapped(camelCase.class, false, true, "NO_SUCH_COLUMN", 1));

        assertEquals("Could not map NO_SUCH_COLUMN to any property.", ex.getMessage());
    }
}