package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.Sql2o;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The first thing worth knowing: whether a reader defined by this extension runs at all.
 *
 * <p>Everything else about the mapping is a matter of agreeing with the reflective path, and that cannot be argued
 * about a class that never loads. So the fixtures here are deliberately the awkward ones — a private field, a class whose
 * constructor is not public, a field with a setter as well, a primitive, and a record — and each has to come back with
 * the values it was written with.
 *
 * <p>The columns are named after the properties on purpose. The naming convention is somebody else's decision and has its
 * own tests in core; what is under test here is the part that writes.
 */
public class ReaderRunsTest {

    private static final String URL = "jdbc:h2:mem:readerruns;DB_CLOSE_DELAY=-1";

    private final Sql2o sql2o = new Sql2o(URL, "sa", "");

    /** A field with no setter and no getter, which is the case a reader reaches most directly. */
    public static class FieldsPojo {
        public String hidden = "nothing yet";
        public int count;
    }

    /** A property with both a setter and the field behind it: the setter has to be the one that is called. */
    public static class SetterPojo {
        private String name;
        private int times;

        public String getName() {
            return name;
        }

        public int getTimes() {
            return times;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    /** A field and a setter that are neither of them private, since generated code cannot reach a private one. */
    public static class ReachableMembers {
        String packageField;
        int packageNumber;

        public void setName(String name) {
            this.packageField = name;
        }

        public String getName() {
            return packageField;
        }
    }

    /** Not public, and neither is its constructor, so only bytecode defined alongside it can build one. */
    static class PackagePrivatePojo {
        String theText;

        PackagePrivatePojo() {
        }
    }

    public record Point(int x, int y, String label) {
    }

    /** Eight columns, so that the slots past the first six are pushed as bytes rather than constants. */
    public static class Wide {
        public String c0;
        public String c1;
        public String c2;
        public String c3;
        public String c4;
        public String c5;
        public String c6;
        public String c7;
    }

    @Test
    public void aFieldIsWrittenAndReadBack() {
        withTable("FIELDSPOJO", "create table FIELDSPOJO (hidden varchar(20), count integer)",
                "insert into FIELDSPOJO values ('written', 7)", "select hidden, count from FIELDSPOJO",
                FieldsPojo.class, rows -> {
                    assertEquals(1, rows.size());
                    assertEquals("written", rows.get(0).hidden);
                    assertEquals(7, rows.get(0).count);
                });
    }

    /**
     * Package private members, which the reader reaches because it is defined into the same package and loaded by the
     * same loader. Private ones are out of reach and are refused instead; see {@code ReaderRefusesTest}.
     */
    @Test
    public void packagePrivateMembersAreWrittenAndReadBack() {
        withTable("REACHABLEMEMBERS", "create table REACHABLEMEMBERS (packageField varchar(30), packageNumber integer)",
                "insert into REACHABLEMEMBERS values ('a package member', 4)",
                "select packageField, packageNumber from REACHABLEMEMBERS", ReachableMembers.class, rows -> {
                    assertEquals(1, rows.size());
                    assertEquals("a package member", rows.get(0).packageField);
                    assertEquals(4, rows.get(0).packageNumber);
                });
    }

    @Test
    public void aSetterIsCalledRatherThanTheFieldWritten() {
        withTable("SETTERPOJO", "create table SETTERPOJO (name varchar(30), times integer)",
                "insert into SETTERPOJO values ('through a setter', 3)", "select name, times from SETTERPOJO",
                SetterPojo.class, rows -> {
                    assertEquals(1, rows.size());
                    assertEquals("through a setter", rows.get(0).getName());
                    assertEquals(3, rows.get(0).getTimes());
                });
    }

    @Test
    public void aPackagePrivateClassIsBuiltByItsOwnNoArgumentConstructor() {
        withTable("PACKAGEPRIVATEPOJO", "create table PACKAGEPRIVATEPOJO (theText varchar(40))",
                "insert into PACKAGEPRIVATEPOJO values ('into a package private class')",
                "select theText from PACKAGEPRIVATEPOJO", PackagePrivatePojo.class, rows -> {
                    assertEquals(1, rows.size());
                    assertEquals("into a package private class", rows.get(0).theText);
                });
    }

    @Test
    public void aRecordIsBuiltThroughItsCanonicalConstructor() {
        withTable("POINT", "create table POINT (x integer, y integer, label varchar(20))",
                "insert into POINT values (3, 4, 'a point')", "select x, y, label from POINT", Point.class, rows -> {
                    assertEquals(1, rows.size());
                    assertEquals(new Point(3, 4, "a point"), rows.get(0));
                });
    }

    /** Eight columns, where the slots past the first six are pushed as bytes rather than loaded as constants. */
    @Test
    public void aWideRowIsWrittenAndReadBack() {
        withTable("WIDE", "create table WIDE (c0 varchar(10), c1 varchar(10), c2 varchar(10), c3 varchar(10),"
                + " c4 varchar(10), c5 varchar(10), c6 varchar(10), c7 varchar(10))",
                "insert into WIDE values ('0', '1', '2', '3', '4', '5', '6', '7')",
                "select c0, c1, c2, c3, c4, c5, c6, c7 from WIDE", Wide.class, rows -> {
                    assertEquals(1, rows.size());
                    final Wide wide = rows.get(0);
                    assertEquals("0", wide.c0);
                    assertEquals("5", wide.c5);
                    assertEquals("6", wide.c6);
                    assertEquals("7", wide.c7);
                });
    }

    @Test
    public void aNullRowKeepsTheDefaultsAndDoesNotUnboxOne() {
        withTable("NULLS", "create table NULLS (name varchar(30), times integer)",
                "insert into NULLS values (null, null)", "select name, times from NULLS", SetterPojo.class, rows -> {
                    assertEquals(1, rows.size());
                    // A null never reaches a primitive: the reflective path returns before assigning, leaving the field
                    // at its default, and a reader that unboxed one would fail the row instead.
                    assertEquals(null, rows.get(0).getName());
                    assertEquals(0, rows.get(0).getTimes());
                });
    }

    private interface Checks<T> {
        void check(List<T> rows);
    }

    /**
     * Fetches through the bytecode factory with no fallback allowed, so that a shape which quietly fell back would fail
     * here instead of passing while proving the opposite of what the test claims.
     */
    private <T> void withTable(String table, String create, String insert, String select, Class<T> type,
                               Checks<T> checks) {
        try (Connection connection = sql2o.open()) {
            drop(connection, table);
            connection.createQuery(create).executeUpdate();
            connection.createQuery(insert).executeUpdate();

            final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
            builder.setQuirks(connection.getSql2o().getQuirks());
            builder.setFallbackAllowed(false);
            final ResultSetHandlerFactory<T> factory = builder.newFactory(type);

            checks.check(connection.createQuery(select).executeAndFetch(factory));
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