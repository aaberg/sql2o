package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.NamingConvention;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.Settings;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.NoQuirks;

import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a reader will not be built for, and why.
 *
 * <p>Each of these is a shape the reflective path maps perfectly well. A refusal is not a defect and not a silent
 * difference: the handler of core answers instead, and the only way to see one is {@code setFallbackAllowed(false)}.
 */
public class ReaderRefusesTest {

    private static final String URL = "jdbc:h2:mem:readerrefuses;DB_CLOSE_DELAY=-1";

    private final Sql2o sql2o = new Sql2o(URL, "sa", "");

    /** Nothing but a private field, so there is no public way in at all. */
    public static class PrivateField {
        private String name;
    }

    /** A public field beside a private setter, and the setter is the one a writer would choose. */
    public static class PrivateSetter {
        public String other;

        private void setName(String name) {
            this.other = name;
        }
    }

    /** Nothing to write through: a getter, and no setter and no field of that name. */
    public static class ReadOnlyProperty {
        public String getComputed() {
            return "computed";
        }
    }

    public static class Plain {
        public int number;
        public String text;
    }

    /** Public members behind a private constructor, which generated code cannot call. */
    public static class PrivateConstructor {
        public String name;

        private PrivateConstructor() {
        }
    }

    /** No no-argument constructor at all, so neither path can build one and the fallback reports it the usual way. */
    public static class NoNoArgConstructor {
        public String name;

        public NoNoArgConstructor(String name) {
            this.name = name;
        }
    }

    /**
     * A private member is out of reach from any class but its own, and declaring the reader a nest member does not help:
     * the jvm requires the class being reached to list the reader among its nest members, and that list is part of the
     * bytes of a class which was loaded long before this ran.
     */
    @Test
    public void aPrivateFieldIsRefusedWithTheReason() {
        assertRefused(PrivateField.class, "name", "private field");
    }

    @Test
    public void aPrivateSetterIsRefusedWithTheReason() {
        assertRefused(PrivateSetter.class, "name", "private setter");
    }

    /** A property with no setter and no field is a shape the reflective path reports on, so it is refused rather than compiled. */
    @Test
    public void aPropertyWithNothingToWriteThroughIsRefused() {
        assertRefused(ReadOnlyProperty.class, "computed", "neither a setter nor a field");
    }

    /** A dotted column name walks into a nested object, which a reader does not do. */
    @Test
    public void aDottedColumnNameIsRefused() {
        assertRefused(Plain.class, "number.inner", "dotted");
    }

    /** A private constructor cannot be called from generated code, for the same reason a private member cannot. */
    @Test
    public void aPrivateConstructorIsRefusedWithTheReason() {
        assertRefused(PrivateConstructor.class, "name", "private");
    }

    /** A class with no no-argument constructor is refused: the reflective path cannot build it either. */
    @Test
    public void aClassWithNoNoArgumentConstructorIsRefused() {
        assertRefused(NoNoArgConstructor.class, "name", "cannot build");
    }

    /**
     * With the fallback on, such a class fails exactly the way core fails it — the fallback hands the result set to
     * the handler of core rather than reimplementing it.
     */
    @Test
    public void withTheFallbackOnSuchAClassFailsExactlyTheWayCoreFailsIt() {
        try (Connection connection = sql2o.open()) {
            drop(connection, "NONOARG");
            connection.createQuery("create table NONOARG (name varchar(40))").executeUpdate();
            connection.createQuery("insert into NONOARG values ('no constructor for this')").executeUpdate();

            final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
            builder.setQuirks(sql2o.getQuirks());

            final String throughTheFallback = describe(() -> connection.createQuery("select name from NONOARG")
                    .executeAndFetch(builder.newFactory(NoNoArgConstructor.class)));
            final String throughCore = describe(() -> connection.createQuery("select name from NONOARG")
                    .executeAndFetch(NoNoArgConstructor.class));

            assertEquals(throughCore, throughTheFallback, "the fallback did not fail the way core fails it");
        }
    }

    /** And the positive case, so that the refusals above are about the shapes rather than about refusing everything. */
    @Test
    public void anOrdinaryShapeIsCompiled() {
        assertTrue(compile(Plain.class, "number", "text").compiled());
    }

    /** Turning the fallback off is how a caller finds out where the bytecode is not being used. */
    @Test
    public void withTheFallbackTurnedOffARefusalIsReportedRatherThanHidden() {
        final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
        builder.setQuirks(sql2o.getQuirks());
        builder.setFallbackAllowed(false);
        final ResultSetHandlerFactory<Plain> factory = builder.newFactory(Plain.class);

        final Sql2oException thrown =
                assertThrows(Sql2oException.class, () -> factory.newResultSetHandler(meta("number", "number.inner")));

        assertTrue(thrown.getMessage().contains("dotted"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Turn setFallbackAllowed on"), thrown.getMessage());
    }

    /**
     * With the fallback left on, the same shape is mapped anyhow — and mapped the way core would have, which is the whole
     * promise of the fallback. A private field is the sharpest case for it: the reader cannot reach one, and the row still
     * arrives in the field.
     */
    @Test
    public void withTheFallbackOnAPrivateFieldIsMappedAnyhow() {
        try (Connection connection = sql2o.open()) {
            drop(connection, "PRIVATEFIELD");
            connection.createQuery("create table PRIVATEFIELD (name varchar(20))").executeUpdate();
            connection.createQuery("insert into PRIVATEFIELD values ('through the fallback')").executeUpdate();

            final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
            builder.setQuirks(sql2o.getQuirks());

            final List<PrivateField> rows =
                    connection.createQuery("select name from PRIVATEFIELD").executeAndFetch(builder.newFactory(PrivateField.class));

            assertEquals(1, rows.size());
            assertEquals("through the fallback", readName(rows.get(0)));
        }
    }

    /** The field is private on purpose, so reading it here is a reflection of the test rather than of the mapping. */
    private static String readName(PrivateField pojo) {
        try {
            final var field = PrivateField.class.getDeclaredField("name");
            field.setAccessible(true);
            return (String) field.get(pojo);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("a field of this test class is readable", e);
        }
    }

    private void assertRefused(Class<?> type, String column, String becauseIt) {
        final RowPlanCompiler.Outcome outcome = compile(type, column);

        assertFalse(outcome.compiled(), column + " was compiled when it should have been refused");
        assertNull(outcome.plan());
        assertTrue(outcome.refusal().contains(becauseIt),
                "expected the reason to mention " + becauseIt + " but it says: " + outcome.refusal());
    }

    private RowPlanCompiler.Outcome compile(Class<?> type, String... columns) {
        final Settings settings = new Settings(new NamingConvention(false, false), new NoQuirks(), true);
        return new RowPlanCompiler(type, settings, columns).compile(Map.of());
    }

    private static String describe(java.util.concurrent.Callable<?> query) {
        try {
            query.call();
            return "no failure";
        } catch (Throwable e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    /**
     * The metadata of a result set, which is all a factory is given before a row.
     *
     * <p>A proxy rather than a stub class: the interface runs to a hundred methods and only two of them are ever asked.
     */
    private static ResultSetMetaData meta(String... columns) {
        return (ResultSetMetaData) java.lang.reflect.Proxy.newProxyInstance(
                ReaderRefusesTest.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnCount".equals(method.getName())) {
                        return columns.length;
                    }
                    if ("getColumnLabel".equals(method.getName())) {
                        return columns[(int) args[0] - 1];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void drop(Connection connection, String table) {
        try {
            connection.createQuery("drop table " + table).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }
}