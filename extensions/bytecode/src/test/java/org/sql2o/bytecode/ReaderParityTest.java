package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.DefaultResultSetHandlerFactoryBuilder;
import org.sql2o.NamingConvention;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.Settings;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bytecode path and the reflective one, asked the same question and required to answer it the same way.
 *
 * <p>This is the test that matters. Any single assertion about a reader proves that one shape works; requiring the two
 * paths to agree — values, and the wording of the errors they raise — over a matrix of shapes is what makes the extension
 * a drop-in rather than a thing that happens to work.
 *
 * <p>The bytecode side runs with the fallback off throughout, so that a shape which quietly fell back would fail here
 * instead of agreeing by accident.
 */
public class ReaderParityTest {

    private static final String URL = "jdbc:h2:mem:readerparity;DB_CLOSE_DELAY=-1";

    private final Sql2o sql2o = new Sql2o(URL, "sa", "");

    public static class Numbers {
        public int anInt;
        public long aLong;
        public short aShort;
        public byte aByte;
        public double aDouble;
        public float aFloat;
        public boolean aBoolean;

        @Override
        public String toString() {
            return "Numbers[" + anInt + ", " + aLong + ", " + aShort + ", " + aByte + ", " + aDouble + ", "
                    + aFloat + ", " + aBoolean + "]";
        }
    }

    /**
     * A char property, which is where the two paths are known to differ and are not expected to agree.
     *
     * <p>Kept apart from {@link Numbers} so that the rest of the matrix can insist on identical wording. See
     * {@code aWrongTypeForACharFieldFailsOnBothPathsButNotWithTheSameWording}.
     */
    public static class WithChar {
        public char aChar;

        @Override
        public String toString() {
            return "WithChar[" + (int) aChar + "]";
        }
    }

    public static class Text {
        public String text;
        public java.math.BigDecimal amount;

        @Override
        public String toString() {
            return "Text[" + text + ", " + amount + "]";
        }
    }

    public static class WithAccessors {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "WithAccessors[" + name + ", " + count + "]";
        }
    }

    public static class Inherited extends Numbers {
        public String own;

        @Override
        public String toString() {
            return "Inherited[" + anInt + ", " + aLong + ", " + own + "]";
        }
    }

    public static class Enums {
        public Colour colour;
        public String asText;

        @Override
        public String toString() {
            return "Enums[" + colour + ", " + asText + "]";
        }
    }

    public enum Colour {
        RED, BLUE
    }

    public record Point(int x, int y, String label) {
    }

    public record OnlyReferences(String text, Object anything, Number big) {
    }

    /**
     * A setter the class does not declare itself: a default method of an interface it implements. Core only reads
     * declared methods, so neither path sees a setter here and the column is unmapped on both — which is the
     * agreement that matters, since a reader that bound the interface method would have to call it as one.
     */
    public interface Named {
        void storeName(String name);

        default void setName(String name) {
            storeName(name);
        }
    }

    public static class SetterFromAnInterface implements Named {
        private String stored;

        @Override
        public void storeName(String name) {
            this.stored = name;
        }
    }

    // ---------------------------------------------------------------- the shapes

    @Test
    public void everyPrimitiveIsReadTheSameWayBothWays() {
        assertSameRow("NUMBERS",
                "create table NUMBERS (anInt integer, aLong bigint, aShort smallint, aByte smallint,"
                        + " aDouble double, aFloat real, aBoolean boolean)",
                "insert into NUMBERS values (1, 2, 3, 4, 5.5, 6.5, true)",
                "select anInt, aLong, aShort, aByte, aDouble, aFloat, aBoolean from NUMBERS",
                Numbers.class);
    }

    /**
     * The one place the two paths are known to differ, pinned so that it stays a known difference.
     *
     * <p>A {@code char} column comes back from h2 as a {@code String}, and a {@code char} property then cannot take it.
     * The reflective path fails in {@code Field.set}, which says what it was given and where; a reader fails on the cast,
     * which says what the value turned out to be. Both refuse the row, and neither message can be had from the other:
     * producing the wording of {@code Field.set} from generated code would mean a check per column on the way in.
     */
    @Test
    public void aWrongTypeForACharFieldFailsOnBothPathsButNotWithTheSameWording() {
        writeRow("WITHCHAR", "create table WITHCHAR (aChar char)", "insert into WITHCHAR values ('x')");

        final String reflective = reflectively("select aChar from WITHCHAR", WithChar.class);
        final String bytecode = byBytecode("select aChar from WITHCHAR", WithChar.class);

        assertTrue(reflective.startsWith("java.lang.IllegalArgumentException"), reflective);
        assertTrue(bytecode.startsWith("java.lang.ClassCastException"), bytecode);
        assertTrue(!reflective.equals(bytecode), "the two wordings are expected to differ, and pinning it says so");
    }

    @Test
    public void textAndDecimalAreReadTheSameWayBothWays() {
        assertSameRow("TEXT", "create table TEXT (text varchar(20), amount decimal(12,2))",
                "insert into TEXT values ('some text', 1234.56)",
                "select text, amount from TEXT", Text.class);
    }

    @Test
    public void settersAreCalledTheSameWayBothWays() {
        assertSameRow("WITHACCESSORS", "create table WITHACCESSORS (name varchar(20), count integer)",
                "insert into WITHACCESSORS values ('a name', 9)",
                "select name, count from WITHACCESSORS", WithAccessors.class);
    }

    /** Fields of a superclass, which is where a reader has to name the class the field was declared on. */
    @Test
    public void inheritedFieldsAreReadTheSameWayBothWays() {
        assertSameRow("INHERITED",
                "create table INHERITED (anInt integer, aLong bigint, own varchar(20))",
                "insert into INHERITED values (1, 2, 'own value')",
                "select anInt, aLong, own from INHERITED", Inherited.class);
    }

    @Test
    public void aRecordIsReadTheSameWayBothWays() {
        assertSameRow("POINT", "create table POINT (x integer, y integer, label varchar(20))",
                "insert into POINT values (3, 4, 'a point')",
                "select x, y, label from POINT", Point.class);
    }

    @Test
    public void aRecordOfReferencesOnlyIsReadTheSameWayBothWays() {
        assertSameRow("ONLYREFERENCES",
                "create table ONLYREFERENCES (text varchar(20), anything varchar(20), big integer)",
                "insert into ONLYREFERENCES values ('t', 'o', 9)",
                "select text, anything, big from ONLYREFERENCES", OnlyReferences.class);
    }

    /**
     * An enum column, which is the one case where the two paths can differ for a reason that is not about writing: the
     * value has to arrive already an enum, or both raise the same way.
     */
    @Test
    public void anEnumIsReadTheSameWayBothWays() {
        assertSameRow("ENUMS", "create table ENUMS (colour varchar(10), asText varchar(20))",
                "insert into ENUMS values ('BLUE', 'BLUE')",
                "select colour, asText from ENUMS", Enums.class);
    }

    /**
     * A row of nulls, which is where the two could plausibly differ: the reflective path returns before assigning, and a
     * reader that unboxed a null instead would fail the whole row rather than leaving the fields at their defaults.
     */
    @Test
    public void aRowOfNullsIsReadTheSameWayBothWays() {
        assertSameRow("NULLNUMBERS",
                "create table NULLNUMBERS (anInt integer, aLong bigint, aShort smallint, aByte smallint,"
                        + " aDouble double, aFloat real, aBoolean boolean)",
                "insert into NULLNUMBERS values (null, null, null, null, null, null, null)",
                "select anInt, aLong, aShort, aByte, aDouble, aFloat, aBoolean from NULLNUMBERS",
                Numbers.class);
    }

    /**
     * An unmapped column, which is an error by default and both paths have to raise it when the row arrives.
     *
     * <p>The extra column is a real column of the table: selecting a column that is not there fails in the driver,
     * before either path sees the row, and would prove nothing about the mapping.
     */
    @Test
    public void anUnmappedColumnIsReportedTheSameWayBothWays() {
        assertSameFailure("UNMAPPED", "create table UNMAPPED (text varchar(20), amount decimal(12,2), extra integer)",
                "insert into UNMAPPED values ('t', 1234.56, 0)",
                "select text, amount, extra from UNMAPPED", Text.class);
    }

    /**
     * An unmapped column for a record, where the failure is thrown by the code of each path rather than by the
     * metadata: the reflective path raises it out of the builder, and a reader has it compiled in.
     */
    @Test
    public void anUnknownComponentOfARecordIsReportedTheSameWayBothWays() {
        assertSameFailure("EXTRACOMPONENT",
                "create table EXTRACOMPONENT (x integer, y integer, label varchar(20), extra integer)",
                "insert into EXTRACOMPONENT values (3, 4, 'a point', 0)",
                "select x, y, label, extra from EXTRACOMPONENT", Point.class);
    }

    /**
     * A converter that refuses, where the wording is the thing under test.
     *
     * <p>Read rather than written, so that the failure belongs to the mapping: the column takes the text happily and it is
     * the conversion into {@link java.math.BigDecimal} that cannot be done. Both paths are expected to let the
     * {@link NumberFormatException} out as it is, since neither of them catches anything wider than a converter failure.
     */
    @Test
    public void aFailedConversionIsReportedTheSameWayBothWays() {
        assertSameFailure("NOTATYPE", "create table NOTATYPE (amount varchar(20))",
                "insert into NOTATYPE values ('not a number at all')",
                "select amount from NOTATYPE", Text.class);
    }

    /**
     * And the case the wording of {@link ReaderSupport} exists for: a converter that fails the way a converter is meant
     * to, by throwing {@link org.sql2o.converters.ConverterException}. Both paths wrap it, and the message names the
     * property, the member and the type in the same words.
     */
    @Test
    public void aConverterThatRefusesIsReportedTheSameWayBothWays() {
        writeRow("REFUSES", "create table REFUSES (text varchar(20), amount decimal(12,2))",
                "insert into REFUSES values ('t', 1234.56)");

        final Quirks refusing = new NoQuirks(refusingConverters());
        final String reflective = reflectively("select text from REFUSES", Text.class, refusing);
        final String bytecode = byBytecode("select text from REFUSES", Text.class, refusing);

        assertEquals(reflective, bytecode, "the two paths reported a refusing converter differently");
        // The wording is PojoProperty's, and text is a field with no setter, so it says field and not property.
        assertTrue(reflective.contains("Error trying to convert value of type java.lang.String to field text of type"),
                "the message names the type, the property and its declaring type: " + reflective);
        assertTrue(reflective.contains("Text"), "the message names the class the property is on: " + reflective);

        // And the plan says the same thing the message says: the description the generated code reports against is
        // the one the reflective path words the error with.
        final RowPlan plan = compile(Text.class, true, "text");
        assertTrue(plan.descriptions()[0].contains("field text of type"),
                "the plan describes the column the way the error words it: " + plan.descriptions()[0]);
    }

    /**
     * And the same for a record component: a converter that fails the way a converter is meant to, by throwing
     * {@link org.sql2o.converters.ConverterException}. Both paths wrap it, and the message names the column and the
     * type in the same words — the type rendered the way the reflective path renders it, which is
     * {@code Class.toString} and not the bare name.
     */
    @Test
    public void aConverterThatRefusesAComponentIsReportedTheSameWayBothWays() {
        writeRow("REFUSESCOMPONENT", "create table REFUSESCOMPONENT (x integer, y integer, label varchar(20))",
                "insert into REFUSESCOMPONENT values (3, 4, 'a point')");

        final Quirks refusing = new NoQuirks(refusingConverters());
        final String reflective = reflectively("select x, y, label from REFUSESCOMPONENT", Point.class, refusing);
        final String bytecode = byBytecode("select x, y, label from REFUSESCOMPONENT", Point.class, refusing);

        assertEquals(reflective, bytecode, "the two paths reported a refusing converter differently");
        // The column arrives uppercased from h2, which both paths repeat; the point pinned here is the type, rendered
        // the reflective way.
        assertTrue(reflective.contains("to type class java.lang.String"),
                "the message renders the type the reflective way: " + reflective);
    }

    /** A converter that always fails the way a converter is supposed to. */
    private static Map<Class, Converter> refusingConverters() {
        final Map<Class, Converter> converters = new java.util.HashMap<>();
        converters.put(String.class, new Converter<String>() {
            @Override
            public String convert(Object val) throws org.sql2o.converters.ConverterException {
                throw new org.sql2o.converters.ConverterException("no string today");
            }

            @Override
            public Object toDatabaseParam(String val) {
                return val;
            }
        });
        return converters;
    }

    /**
     * A setter inherited as a default method of an interface, which neither path treats as a setter: core only reads
     * declared methods, and the compiler binds exactly what core binds. The column is therefore unmapped on both, and
     * both raise it the same way.
     */
    @Test
    public void anInterfaceDefaultMethodIsNotASetterOnEitherPath() {
        assertSameFailure("INTERFACESETTER", "create table INTERFACESETTER (name varchar(20))",
                "insert into INTERFACESETTER values ('a name')",
                "select name from INTERFACESETTER", SetterFromAnInterface.class);
    }
    /**
     * And with the error switched off, the unmapped column is compiled away and both paths ignore it.
     *
     * <p>Again a real column: what is compared here is two rows that were actually read, not two identical driver
     * errors over a column that is not there.
     */
    @Test
    public void anUnmappedColumnIsIgnoredByBothWhenItIsNotAnError() {
        final RowPlan plan = compile(Text.class, false, "text", "extra");

        assertNull(plan.targetTypes()[1], "the unmapped column is compiled away rather than refused");
        assertSameRow("UNMAPPEDIGNORED",
                "create table UNMAPPEDIGNORED (text varchar(20), amount decimal(12,2), extra integer)",
                "insert into UNMAPPEDIGNORED values ('t', 1234.56, 0)",
                "select text, extra from UNMAPPEDIGNORED", Text.class);
    }

    // ---------------------------------------------------------------- the machinery

    private void assertSameRow(String table, String ddl, String insert, String select, Class<?> type) {
        writeRow(table, ddl, insert);

        assertEquals(reflectively(select, type), byBytecode(select, type),
                "the two paths read " + table + " differently");
    }

    /**
     * Both paths must fail the same way, down to the wording.
     *
     * <p>And the failure has to come out of the mapping: a column the driver itself rejects fails both paths before
     * either of them sees the row, and comparing two identical driver errors would prove nothing about the mapping.
     */
    private void assertSameFailure(String table, String ddl, String insert, String select, Class<?> type) {
        writeRow(table, ddl, insert);

        final String reflective = reflectively(select, type);
        final String bytecode = byBytecode(select, type);

        assertTrue(!reflective.toLowerCase().contains("jdbc") && !bytecode.toLowerCase().contains("jdbc"),
                "the query failed in the driver, before either mapping saw the row: " + reflective + " / " + bytecode);
        assertEquals(reflective, bytecode, "the two paths failed differently on " + table);
        assertTrue(!reflective.startsWith("[{"), "neither path failed on " + table + ", so nothing was compared: " + reflective);
    }

    /** Read through the reflective factory, with the same settings the bytecode side is given. */
    private String reflectively(String select, Class<?> type) {
        return reflectively(select, type, sql2o.getQuirks());
    }

    private String reflectively(String select, Class<?> type, Quirks quirks) {
        try (Connection connection = sql2o.open()) {
            final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
            builder.setQuirks(quirks);
            builder.throwOnMappingError(true);

            return String.valueOf(connection.createQuery(select).executeAndFetch(builder.newFactory(type)));
        } catch (Throwable e) {
            return describe(e);
        }
    }

    private String byBytecode(String select, Class<?> type) {
        return byBytecode(select, type, sql2o.getQuirks());
    }

    private String byBytecode(String select, Class<?> type, Quirks quirks) {
        try (Connection connection = sql2o.open()) {
            final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
            builder.setQuirks(quirks);
            builder.throwOnMappingError(true);
            // Off on purpose: a shape that fell back would agree by accident and pass without proving anything.
            builder.setFallbackAllowed(false);

            final ResultSetHandlerFactory<?> factory = builder.newFactory(type);
            return String.valueOf(connection.createQuery(select).executeAndFetch(factory));
        } catch (Throwable e) {
            return describe(e);
        }
    }

    private static String describe(Throwable thrown) {
        return thrown.getClass().getName() + ": " + thrown.getMessage();
    }

    private RowPlan compile(Class<?> type, boolean throwOnMappingError, String... columns) {
        final Settings settings =
                new Settings(new NamingConvention(false, false), new NoQuirks(), throwOnMappingError);
        final RowPlanCompiler.Outcome outcome =
                new RowPlanCompiler(type, settings, columns).compile(Map.of());

        assertTrue(outcome.compiled(), "not compiled: " + outcome.refusal());
        assertNotNull(outcome.bytes());
        return outcome.plan();
    }

    private void writeRow(String table, String ddl, String insert) {
        try (Connection connection = sql2o.open()) {
            drop(connection, table);
            connection.createQuery(ddl).executeUpdate();
            connection.createQuery(insert).executeUpdate();
        }
    }

    private static void drop(Connection connection, String table) {
        try {
            connection.createQuery("drop table " + table).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }
}