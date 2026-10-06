package org.sql2o.params;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Binding a value with its type named, the way {@link org.sql2o.Query#addParameter(String, Class, Object)} does, and
 * reading it back out of a real database.
 *
 * <p>This is the base of one test per database. A database supplies its connection and says what a column of a given
 * kind looks like in its dialect, and may leave out the kinds it has no way of expressing. What is left out is written
 * down rather than guessed at, since "the database cannot hold this" and "sql2o loses this" are different facts and
 * only one of them is a defect.
 *
 * <p>The types here are the ones a caller actually writes in a parameter. What a type means once it has been converted
 * is the business of the converters, and those are tested on their own; what is pinned here is that naming the type
 * gets the value into the database and back out again unchanged.
 */
public abstract class AbstractTypedParameterTest {

    /** How much text a clob test writes: past what any of the databases will keep inline in a varchar. */
    private static final int CLOB_LENGTH = 20_000;

    /** A logical column kind, named so that a database can say what it looks like without this class knowing. */
    protected static final String STRING = "string";
    protected static final String SMALL = "smallint";
    protected static final String INTEGER = "integer";
    protected static final String BIGINT = "bigint";
    protected static final String REAL = "real";
    protected static final String DOUBLE = "double";
    protected static final String BOOLEAN = "boolean";
    protected static final String DECIMAL = "decimal";
    protected static final String DATE = "date";
    protected static final String TIME = "time";
    protected static final String TIMESTAMP = "timestamp";
    /** Named apart from the type it stands for, which a constant called UUID would shadow. */
    protected static final String UUID_KIND = "uuid";
    protected static final String BLOB = "blob";
    /** Text too large to sit inline, which is a different thing from the bytes of a blob. */
    protected static final String CLOB = "clob";

    protected enum Colour {
GREEN, BLUE
}

    /**
     * One value of one type, and the kind of column it needs. The expected value is what reading it back should give,
     * which is not always the value that went in: a driver may hand back a subclass, and a database may not keep every
     * digit of what it was given.
     */
    protected record ParameterCase(String label, Class<?> type, Object value, Object expected, String columnKind) {}

    /** The database under test. Supplied by the subclass, and the only thing it has to. */
    protected abstract Sql2o sql2o();

    /** How this database spells a column of the given kind, precision and scale included where it takes them. */
    protected abstract String columnType(String columnKind);

    /** A name of its own, so that the table of one database is not the table of another. */
    protected String tableName() {
        return "TYPEDPARAMS";
    }

    /**
     * The name of the wide byte column in the statements below, which a database may reserve: {@code blob} is a
     * reserved word in MySQL and MariaDB, which quote it, while everywhere else it stands as it is.
     */
    protected String blobColumn() {
        return "blob";
    }

    /** The name of the wide text column; see {@link #blobColumn()}. */
    protected String clobColumn() {
        return "clob";
    }

    /**
     * The kinds this database has no way of expressing. Overridden only where there is a reason, and the reason belongs
     * in the comment of the override.
     */
    protected List<ParameterCase> cases() {
        Timestamp aMoment = Timestamp.valueOf("2020-01-01 12:34:56.789");
        return new ArrayList<>(List.of(
                new ParameterCase("String", String.class, "hello", "hello", STRING),
                new ParameterCase("Integer", Integer.class, 42, 42, INTEGER),
                new ParameterCase("int", int.class, 42, 42, INTEGER),
                new ParameterCase("Long", Long.class, 42L, 42L, BIGINT),
                new ParameterCase("Short", Short.class, (short) 7, (short) 7, SMALL),
                new ParameterCase("Byte", Byte.class, (byte) 3, (byte) 3, SMALL),
                new ParameterCase("Double", Double.class, 1.5d, 1.5d, DOUBLE),
                new ParameterCase("Float", Float.class, 1.5f, 1.5f, REAL),
                new ParameterCase("Boolean", Boolean.class, Boolean.TRUE, Boolean.TRUE, BOOLEAN),
                // Labelled apart from the one above, since a column is named after its case.
                new ParameterCase("boolean primitive", boolean.class, false, Boolean.FALSE, BOOLEAN),
                // What comes back carries the scale of the column rather than the scale it went in with, and
                // BigDecimal.equals compares scale as well as value, so each decimal case says what its column has.
                new ParameterCase("BigDecimal", BigDecimal.class, new BigDecimal("1234.56"),
                        new BigDecimal("1234.5600"), DECIMAL),
                new ParameterCase("java.sql.Date", java.sql.Date.class, java.sql.Date.valueOf("2020-01-01"),
                        java.sql.Date.valueOf("2020-01-01"), DATE),
                new ParameterCase("java.sql.Time", java.sql.Time.class, java.sql.Time.valueOf("12:34:56"),
                        java.sql.Time.valueOf("12:34:56"), TIME),
                new ParameterCase("Timestamp", Timestamp.class, aMoment, aMoment, TIMESTAMP),
                new ParameterCase("java.util.Date", Date.class, new Date(aMoment.getTime()), aMoment, TIMESTAMP),
                new ParameterCase("LocalDate", LocalDate.class, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1), DATE),
                new ParameterCase("LocalTime", LocalTime.class, LocalTime.of(12, 34, 56), LocalTime.of(12, 34, 56),
                        TIME),
                new ParameterCase("LocalDateTime", LocalDateTime.class, LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789000000),
                        LocalDateTime.of(2020, 1, 1, 12, 34, 56, 789000000), TIMESTAMP),
                new ParameterCase("Instant", Instant.class, aMoment.toInstant(), aMoment.toInstant(), TIMESTAMP),
                new ParameterCase("OffsetDateTime", OffsetDateTime.class,
                        LocalDateTime.of(2020, 1, 1, 12, 34, 56).atOffset(offsetOfTheJvm()),
                        LocalDateTime.of(2020, 1, 1, 12, 34, 56).atOffset(offsetOfTheJvm()), TIMESTAMP),
                new ParameterCase("OffsetTime", OffsetTime.class, LocalTime.of(12, 34, 56).atOffset(
                                offsetOfTheJvm()), LocalTime.of(12, 34, 56).atOffset(offsetOfTheJvm()), TIME),
                new ParameterCase("UUID", java.util.UUID.class, UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
                        UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), UUID_KIND),
                new ParameterCase("enum", Colour.class, Colour.GREEN, Colour.GREEN, STRING)));
    }

    private static ZoneOffset offsetOfTheJvm() {
        return OffsetDateTime.now(ZoneId.systemDefault()).getOffset();
    }

    /**
     * A blob, which does not go through the type of the value but through the value itself: a byte array is bound as
     * one and an input stream as the other.
     */
    protected byte[] blobValue() {
        return new byte[]{0, 1, 2, 3, (byte) 250, (byte) 255};
    }

    /**
     * Text for a clob: long enough that a database would refuse to keep it inline, and made of a repeating pattern so
     * that a truncated read cannot pass for a whole one by being short.
     */
    protected String clobValue() {
        StringBuilder text = new StringBuilder(CLOB_LENGTH);
        for (int i = 0; i < CLOB_LENGTH; i++) {
            text.append((char) ('a' + i % 26));
        }
        return text.toString();
    }

    private void createTheTable() {
        StringBuilder ddl = new StringBuilder("create table " + tableName() + " (");
        List<String> used = new ArrayList<>();
        for (ParameterCase testCase : cases()) {
            String column = columnName(testCase);
            if (!used.add(column)) {
                // Two cases sharing a column would fight over it, so the case names have to stay distinct.
                throw new IllegalStateException("two cases would share the column " + column);
            }
            ddl.append(column).append(' ').append(columnType(testCase.columnKind()));
            ddl.append(", ");
        }
        ddl.append(blobColumn()).append(' ').append(columnType(BLOB)).append(", ");
        ddl.append(clobColumn()).append(' ').append(columnType(CLOB)).append(')');

        try (Connection connection = sql2o().open()) {
            connection.createQuery(ddl.toString()).executeUpdate();
        }
    }

    /**
     * Drops and recreates the table. A lifecycle method cannot do this instead: it runs once for a whole
     * {@link TestFactory}, and one shared table would leave every case reading the row the first case wrote.
     */
    private void freshTable() {
        dropTheTable();
        createTheTable();
    }

    private void dropTheTable() {
        try (Connection connection = sql2o().open()) {
            connection.createQuery("drop table " + tableName()).executeUpdate();
        } catch (RuntimeException ignored) {
            // Db2 has no drop table if exists, and the table may not be there, which is the state this is called in.
        }
    }

    /** Every value bound with its type named, then read back as the same type. */
    @TestFactory
    public Stream<DynamicTest> aTypedParameterSurvivesTheRoundTrip() {
        return cases().stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.label() + " round trips",
                () -> assertRoundTrip(testCase)));
    }

    /** And into a where clause, where the database has to weigh the value against a column rather than store it. */
    @TestFactory
    public Stream<DynamicTest> aTypedParameterFiltersInAWhereClause() {
        return cases().stream()
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.label() + " filters",
                        () -> assertFilters(testCase)));
    }

    /**
     * Naming the type must not change what arrives.
     *
     * <p>The two ways are not as different as they look: {@code addParameter(String, Object)} hands the value's own
     * class straight to the typed overload, so for a boxed type both ways take the same branch and there is nothing to
     * disagree about. They part company where the class named is not the class of the value, which is every primitive,
     * because {@code int.class} is not {@code Integer.class} and so matches none of the branches the overload has.
     * Those are the ones worth pinning: the typed way has to fall through to the same binding rather than to a
     * {@code setObject} of a wrapper.
     */
    @TestFactory
    public Stream<DynamicTest> namingTheTypeBindsTheSameValueAsNotNamingIt() {
        return Stream.of(
                // The pairs where naming the type picks a different branch, and the boxed type of the same value.
                caseOf(int.class, 42),
                caseOf(long.class, 42L),
                caseOf(short.class, (short) 7),
                caseOf(byte.class, (byte) 3),
                caseOf(double.class, 1.5d),
                caseOf(float.class, 1.5f),
                caseOf(boolean.class, false),
                caseOf(Integer.class, 42),
                caseOf(Long.class, 42L),
                caseOf(String.class, "hello"),
                caseOf(Timestamp.class, Timestamp.valueOf("2020-01-01 12:34:56.789")),
                caseOf(java.sql.Time.class, java.sql.Time.valueOf("12:34:56")),
                caseOf(Integer.class, null),
                caseOf(String.class, null))
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.label() + " binds the same either way",
                        () -> assertTypedAndUntypedAgree(testCase)));
    }

    /**
     * A null, which every type has to carry in and give back.
     *
     * <p>The table is made once for the whole set rather than per case, which is safe here and nowhere else: what is
     * asserted is that the column reads null, and every row written here has a null in every column but its own, so
     * which row the select lands on cannot change the answer.
     */
    @TestFactory
    public Stream<DynamicTest> aNullIsBoundAndReadsBackAsNull() {
        freshTable();

        return cases().stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.label() + " takes a null in and gives a null back",
                () -> assertNullRoundTrip(testCase)));
    }

    /**
     * A clob, which is text too large to sit inline, written and read through the ordinary calls.
     *
     * <p>Written three ways, which is the counterpart of the two a blob has: as a String with the type named, as a String
     * without, and as a stream of characters. The stream is the one worth having, since text of a size worth a clob is
     * rarely text a caller already has in memory.
     */
    @TestFactory
    public Stream<DynamicTest> aClobIsWrittenAndReadBack() {
        return Stream.of(
                DynamicTest.dynamicTest("a clob bound with its type named", this::assertClobBoundWithItsType),
                DynamicTest.dynamicTest("a clob bound without naming its type", this::assertClobBoundWithoutItsType),
                DynamicTest.dynamicTest("a clob bound as a reader", this::assertClobBoundAsReader),
                DynamicTest.dynamicTest("a clob bound as an untyped reader", this::assertClobBoundAsAnUntypedReader));
    }

    /**
     * The two big columns read into fields, which is how an application meets them rather than as a scalar. A clob
     * belongs in a String and a blob in a byte array, and it is the converter rather than the driver that has to work
     * that out, since the database hands both of them back as a handle.
     */
    @TestFactory
    public Stream<DynamicTest> aBigColumnIsReadIntoAField() {
        return Stream.of(
                DynamicTest.dynamicTest("a clob column lands in a String field", this::assertClobReadIntoAField),
                DynamicTest.dynamicTest("a blob column lands in a byte array field", this::assertBlobReadIntoAField));
    }

    /** A blob bound as a byte array and as a stream, which are the two ways there are. */
    @TestFactory
    public Stream<DynamicTest> aBlobBindsBothWays() {
        return Stream.of(
                DynamicTest.dynamicTest("a blob bound as a byte array", this::assertBlobAsByteArray),
                DynamicTest.dynamicTest("a blob bound as a stream", this::assertBlobAsStream));
    }

    /**
     * An instant read into a field of that type, which is the way a value of that kind actually meets an application.
     *
     * <p>Written as a {@link Timestamp} on purpose: what is under test is the read, so the write must not be the thing
     * that can work or fail, and a {@link Timestamp} goes into a timestamp column on every one of these databases
     * whatever else it makes of a {@link Instant}. Which is the whole point of asking separately from the round trip: the
     * read has nothing of sql2o's own in it but the converter, and what the database hands over is worth seeing.
     */
    @TestFactory
    public Stream<DynamicTest> anInstantColumnLandsInAField() {
        return Stream.of(DynamicTest.dynamicTest("an instant column lands in a field of type Instant",
                this::assertInstantReadIntoAField));
    }

    private ParameterCase caseOf(Class<?> type, Object value) {
        return new ParameterCase(type.getSimpleName(), type, value, null, null);
    }

    /** The case of one type, for the tests that are about a single type rather than about the whole matrix. */
    private ParameterCase theCaseFor(Class<?> type) {
        return cases().stream()
                .filter(candidate -> type.equals(candidate.type()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("the matrix has to keep a " + type.getSimpleName() + " case"));
    }

    /** A row with an instant in it, as an application would declare the field. */
    protected static class RowWithInstant {
        public Instant theInstant;
    }

    private void assertInstantReadIntoAField() {
        ParameterCase testCase = theCaseFor(Instant.class);
        String column = columnName(testCase);

        freshTable();

        // Written as a java.sql value, so that the write is not what this is testing.
        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("insert into " + tableName() + " (" + column + ") values (:v)");
            query.addParameter("v", Timestamp.class, Timestamp.from((Instant) testCase.value()));
            query.executeUpdate();
        }

        Instant read;
        try (Connection connection = sql2o().open()) {
            read = connection.createQuery("select " + column + " as theInstant from " + tableName())
                    .executeAndFetchFirst(RowWithInstant.class).theInstant;
        }

        assertEqualsWithAReadableMessage(testCase.value(), read, "an Instant column read into an Instant field");
    }

    private void assertNullRoundTrip(ParameterCase testCase) {
        String column = columnName(testCase);

        // The null goes in first, so that the row it writes is the row a plain select reads back.
        insertRow(testCase, column, null);

        assertColumnReadsBackAs(testCase, whatANullReadsBackAs(testCase.type()), column,
                "a null " + testCase.label() + " read back as " + testCase.type().getSimpleName());

        // A value alongside the null, so that the where clause below has something it could have matched.
        insertRow(testCase, column, testCase.value());

        int matches;
        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("select count(*) from " + tableName() + " where " + column + " = :value");
            bindWithItsType(query, "value", testCase.type(), null);
            matches = query.executeScalar(Integer.class);
        }

        assertEqualsWithAReadableMessage(0, matches,
                "a null " + testCase.label() + " matching no row, not even the one it was written into");
    }

    private void assertColumnReadsBackAs(ParameterCase testCase, Object expected, String column, String what) {
        Object read;
        try (Connection connection = sql2o().open()) {
            read = connection.createQuery("select " + column + " from " + tableName()).executeScalar(testCase.type());
        }

        assertEqualsWithAReadableMessage(expected, read, what);
    }

    /**
     * What a null gives back. For most types that is nothing at all, and for {@code int} it is a zero, since a number
     * is asked for and a zero is what a missing number comes back as. A {@code boolean} is the odd one out and gives
     * nothing rather than a false, which is worth knowing before a field of that type is compared.
     */
    private static Object whatANullReadsBackAs(Class<?> type) {
        return type == int.class ? 0 : null;
    }

    private void assertClobBoundWithItsType() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("insert into " + tableName() + " (" + clobColumn() + ") values (:v)");
            bindWithItsType(query, "v", String.class, clobValue());
            query.executeUpdate();
        }

        assertClobReadsBackWhole();
    }

    private void assertClobBoundWithoutItsType() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            connection.createQuery("insert into " + tableName() + " (" + clobColumn() + ") values (:v)")
                    .addParameter("v", clobValue())
                    .executeUpdate();
        }

        assertClobReadsBackWhole();
    }

    private void assertClobBoundAsReader() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("insert into " + tableName() + " (" + clobColumn() + ") values (:v)");
            bindWithItsType(query, "v", Reader.class, new StringReader(clobValue()));
            query.executeUpdate();
        }

        assertClobReadsBackWhole();
    }

    /** The untyped call as well, since that one arrives at the driver without its type having been named at all. */
    private void assertClobBoundAsAnUntypedReader() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            connection.createQuery("insert into " + tableName() + " (" + clobColumn() + ") values (:v)")
                    .addParameter("v", new StringReader(clobValue()))
                    .executeUpdate();
        }

        assertClobReadsBackWhole();
    }

    private void assertClobReadsBackWhole() {
        String read;
        try (Connection connection = sql2o().open()) {
            read = connection.createQuery("select " + clobColumn() + " from " + tableName()).executeScalar(String.class);
        }

        assertEqualsWithAReadableMessage(clobValue().length(), read == null ? -1 : read.length(), "the clob length");
        assertEqualsWithAReadableMessage(clobValue(), read, "the clob itself");
    }

    private void assertClobReadIntoAField() {
        freshTable();
        writeTheBigColumns();

        BigColumns row = readTheBigColumns();

        assertEqualsWithAReadableMessage(clobValue(), row.clob, "a clob column read into a String field");
    }

    private void assertBlobReadIntoAField() {
        freshTable();
        writeTheBigColumns();

        BigColumns row = readTheBigColumns();

        assertEqualsWithAReadableMessage(blobValue().length, row.blob == null ? -1 : row.blob.length,
                "the blob length read into a byte array field");
        assertArrayEqualsWithAReadableMessage(blobValue(), row.blob, "a blob column read into a byte array field");
    }

    private void writeTheBigColumns() {
        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery(
                    "insert into " + tableName() + " (" + clobColumn() + ", " + blobColumn() + ") values (:clob, :blob)");
            bindWithItsType(query, "clob", String.class, clobValue());
            bindWithItsType(query, "blob", byte[].class, blobValue());
            query.executeUpdate();
        }
    }

    private BigColumns readTheBigColumns() {
        try (Connection connection = sql2o().open()) {
            return connection.createQuery("select " + clobColumn() + ", " + blobColumn() + " from " + tableName())
                    .executeAndFetchUnique(BigColumns.class);
        }
    }

    /** The two big columns as an application would declare them: the text as a String, the bytes as a byte array. */
    protected static class BigColumns {
        public String clob;
        public byte[] blob;
    }

    private void assertRoundTrip(ParameterCase testCase) {
        String column = columnName(testCase);

        freshTable();

        insertRow(testCase, column);

        Object read;
        try (Connection connection = sql2o().open()) {
            read = connection.createQuery("select " + column + " from " + tableName()).executeScalar(testCase.type());
        }

        assertEqualsWithAReadableMessage(testCase.expected(), read,
                "a " + testCase.label() + " read back as " + testCase.type().getSimpleName());
    }

    private void assertFilters(ParameterCase testCase) {
        String column = columnName(testCase);

        freshTable();


        // Two rows, one written with the value and one with a value of the same shape but not the same, so that a
        // parameter which arrives broken shows up as no match rather than as a lucky one.
        insertRow(testCase, column, testCase.value());
        insertRow(testCase, column, otherValueFor(testCase));

        int matches;
        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("select count(*) from " + tableName() + " where " + column + " = :value");
            bindWithItsType(query, "value", testCase.type(), testCase.value());
            matches = query.executeScalar(Integer.class);
        }

        assertEqualsWithAReadableMessage(1, matches,
                "a " + testCase.label() + " matching only the row written with it");
    }

    private void assertTypedAndUntypedAgree(ParameterCase testCase) {
        Object typed = readBackBindingWith(testCase, true);
        Object untyped = readBackBindingWith(testCase, false);

        assertEqualsWithAReadableMessage(untyped, typed,
                "a " + testCase.label() + " bound with and without naming its type");
    }

    private Object readBackBindingWith(ParameterCase testCase, boolean nameTheType) {
        String column = columnName(theCaseFor(String.class));
        freshTable();

        try (Connection connection = sql2o().open()) {
            var query = connection.createQuery("insert into " + tableName() + " (" + column + ") values (:v)");
            if (nameTheType) {
                bindWithItsType(query, "v", testCase.type(), testCase.value());
            } else {
                query.addParameter("v", testCase.value());
            }
            query.executeUpdate();
        }

        try (Connection connection = sql2o().open()) {
            return connection.createQuery("select " + column + " from " + tableName()).executeScalar(String.class);
        }
    }

    private void assertBlobAsByteArray() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            connection.createQuery("insert into " + tableName() + " (" + blobColumn() + ") values (:v)")
                    .addParameter("v", byte[].class, blobValue())
                    .executeUpdate();
        }

        try (Connection connection = sql2o().open()) {
            byte[] read = connection.createQuery("select " + blobColumn() + " from " + tableName()).executeScalar(byte[].class);

            assertEqualsWithAReadableMessage(blobValue().length, read == null ? -1 : read.length, "the blob length");
            for (int i = 0; read != null && i < read.length; i++) {
                assertEqualsWithAReadableMessage(blobValue()[i], read[i], "byte " + i + " of the blob");
            }
        }
    }

    private void assertBlobAsStream() {
        freshTable();

        try (Connection connection = sql2o().open()) {
            connection.createQuery("insert into " + tableName() + " (" + blobColumn() + ") values (:v)")
                    .addParameter("v", InputStream.class, new ByteArrayInputStream(blobValue()))
                    .executeUpdate();
        }

        try (Connection connection = sql2o().open()) {
            byte[] read = connection.createQuery("select " + blobColumn() + " from " + tableName()).executeScalar(byte[].class);

            assertEqualsWithAReadableMessage(blobValue().length, read == null ? -1 : read.length, "the blob length");
            for (int i = 0; read != null && i < read.length; i++) {
                assertEqualsWithAReadableMessage(blobValue()[i], read[i], "byte " + i + " of the blob");
            }
        }
    }

    private void insertRow(ParameterCase testCase, String column) {
        insertRow(testCase, column, testCase.value());
    }

    private void insertRow(ParameterCase testCase, String column, Object value) {
        try (Connection connection = sql2o().open()) {
            Query query = connection.createQuery("insert into " + tableName() + " (" + column + ") values (:v)");
            bindWithItsType(query, "v", testCase.type(), value);
            query.executeUpdate();
        }
    }

    /**
     * Calls the overload that takes a type together with a value of it. Passing a {@code Class<?>} and a plain
     * {@code Object} is enough for {@code addParameter(String, Class, Object)} to miss that overload and land on
     * {@code addParameter(String, Object...)}, which binds an array and so expects as many placeholders as it has
     * elements. The cast is what pins the overload down.
     */
    @SuppressWarnings("unchecked")
    private static void bindWithItsType(Query query, String name, Class<?> type, Object value) {
        // Safe because the cast is erased: Class carries no type parameter at runtime, so this passes the very
        // same object on and the only thing it buys is T = Object, which is the overload that takes the pair.
        query.addParameter(name, (Class<Object>) type, value);
    }

    /** A value that must not match the one written, chosen so that it is of the same shape but not the same. */
    private Object otherValueFor(ParameterCase testCase) {
        if (testCase.value() instanceof Integer) {
            return -1;
        }
        if (testCase.value() instanceof Long) {
            return -1L;
        }
        if (testCase.value() instanceof Short) {
            return (short) -1;
        }
        if (testCase.value() instanceof Byte) {
            return (byte) -1;
        }
        if (testCase.value() instanceof Double) {
            return -1.5d;
        }
        if (testCase.value() instanceof Float) {
            return -1.5f;
        }
        if (testCase.value() instanceof Boolean) {
            return !((Boolean) testCase.value());
        }
        if (testCase.value() instanceof BigDecimal) {
            return new BigDecimal("-1.5");
        }
        if (testCase.value() instanceof String) {
            return "something else entirely";
        }
        if (testCase.value() instanceof Timestamp) {
            return Timestamp.valueOf("1999-12-31 23:59:59");
        }
        if (testCase.value() instanceof java.sql.Date) {
            return java.sql.Date.valueOf("1999-12-31");
        }
        if (testCase.value() instanceof java.sql.Time) {
            return java.sql.Time.valueOf("23:59:59");
        }
        if (testCase.value() instanceof Date) {
            return new Date(0);
        }
        if (testCase.value() instanceof LocalDate) {
            return LocalDate.of(1999, 12, 31);
        }
        if (testCase.value() instanceof LocalTime) {
            return LocalTime.of(23, 59, 59);
        }
        if (testCase.value() instanceof LocalDateTime) {
            return LocalDateTime.of(1999, 12, 31, 23, 59, 59);
        }
        if (testCase.value() instanceof Instant) {
            return Instant.EPOCH;
        }
if (testCase.value() instanceof OffsetDateTime) {
            return LocalDateTime.of(1999, 12, 31, 23, 59, 59).atOffset(OffsetTime.now().getOffset());
        }
        if (testCase.value() instanceof OffsetTime) {
            return LocalTime.of(23, 59, 59).atOffset(OffsetTime.now().getOffset());
        }
        if (testCase.value() instanceof UUID) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        if (testCase.value() instanceof Enum<?> constant) {
            return Arrays.stream(constant.getDeclaringClass().getEnumConstants())
                    .filter(candidate -> candidate != constant)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(constant + " is the only constant, so there is nothing to tell it apart from"));
        }
        // A case added without a second value would otherwise be measured against a value of the wrong shape, and
        // the failure would look like a database problem rather than like the gap in this method.
        throw new IllegalStateException("no second value for a " + testCase.label());
    }

    private String columnName(ParameterCase testCase) {
        return testCase.label().replaceAll("[^A-Za-z0-9]", "_").toLowerCase() + "_col";
    }

    private static void assertEqualsWithAReadableMessage(Object expected, Object actual, String what) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            return;
        }
        throw new AssertionError(what + ": expected [" + expected + "] (" + className(expected) + ") but read ["
                + actual + "] (" + className(actual) + ")");
    }

    private static void assertArrayEqualsWithAReadableMessage(byte[] expected, byte[] actual, String what) {
        if (expected == null ? actual == null : java.util.Arrays.equals(expected, actual)) {
            return;
        }
        throw new AssertionError(what + ": expected " + expected.length + " bytes but read "
                + (actual == null ? "null" : actual.length + " bytes"));
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
