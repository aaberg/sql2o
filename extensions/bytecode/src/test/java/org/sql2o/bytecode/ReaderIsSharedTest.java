package org.sql2o.bytecode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.sql2o.Connection;
import org.sql2o.ResultSetHandler;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader is shared, so what it must not hold is worth pinning here.
 *
 * <p>Three promises, each of which the design rests on: the reader keeps no state between rows, the converters it uses
 * are resolved when a result set is rather than when it was compiled, and the cache key leaves the quirks out so that one
 * compiled reader serves every {@code Sql2o}. Take any of the three away and the others start to lie.
 */
public class ReaderIsSharedTest {

    private static final String URL = "jdbc:h2:mem:readershared;DB_CLOSE_DELAY=-1";

    private final Sql2o sql2o = new Sql2o(URL, "sa", "");

    public static class Pojo {
        public String text;
        public int number;
    }

    /** Counts how often it is asked to convert, and records what it was asked to convert. */
    public static class CountingConverter implements Converter<Object> {

        static final AtomicInteger CALLS = new AtomicInteger();
        static final List<String> SEEN = new ArrayList<>();
        static volatile String tag = "first";

        @Override
        public Object convert(Object val) {
            CALLS.incrementAndGet();
            SEEN.add(tag);
            return val;
        }

        @Override
        public Object toDatabaseParam(Object val) {
            return val;
        }
    }

    private static final String DDL = "create table SHARED (text varchar(20), number integer)";
    private static final String INSERT = "insert into SHARED values ('a row', 1)";
    private static final String SELECT = "select text, number from SHARED";

    @AfterEach
    public void forgetTheCache() {
        ReaderCache.clear();
        CountingConverter.CALLS.set(0);
        CountingConverter.SEEN.clear();
        CountingConverter.tag = "first";
    }

    /**
     * The reader is one instance for every query of a shape, so it may not carry a value from one row into the next. Two
     * handlers built from one plan are used alternately here, which is the case a field holding the object being built
     * would get wrong only on the second row of the second result set.
     */
    @Test
    public void oneReaderServesTwoResultSetsWithoutCarryingAnythingBetweenThem() throws Exception {
        writeRow();

        final RowPlan plan = planFor(Pojo.class, SELECT);
        final RowPlan sameShape = planFor(Pojo.class, SELECT);
        assertSame(plan.reader(), sameShape.reader(), "the same shape has to be compiled once");

        final var firstHandler = handlerFor(plan);
        final var secondHandler = handlerFor(plan);

        assertEquals("a row", readOneText(firstHandler));
        assertEquals("a row", readOneText(secondHandler));

        // And back to the first, which is where a reader holding the object it last built would show up.
        assertEquals("a row", readOneText(firstHandler));
    }

    /** One row through a handler, on a result set of its own, which is what a query gives it. */
    private static Object readOne(ResultSetHandler<Object> handler) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet rs = connection.createStatement().executeQuery(SELECT)) {
            rs.next();
            return handler.handle(rs);
        }
    }

    /** As above, for the readers whose result is a {@link Pojo} of this test's own. */
    private static String readOneText(ResultSetHandler<Object> handler) throws SQLException {
        return ((Pojo) readOne(handler)).text;
    }

    /**
     * The converters are handed in per result set rather than compiled in, so one that is registered after the reader was
     * compiled is still the one that runs.
     */
    @Test
    public void aConverterRegisteredAfterCompilationIsStillUsed() {
        writeRow();

        final RowPlan plan = planFor(Pojo.class, SELECT);

        final Map<Class, Converter> converters = new java.util.HashMap<>();
        converters.put(String.class, new CountingConverter());
        final Quirks secondQuirks = new NoQuirks(converters);

        // The same compiled reader, and a second result set whose quirks know about a converter the first one did not.
        readWith(plan, new NoQuirks());
        final int afterFirst = CountingConverter.CALLS.get();
        readWith(plan, secondQuirks);

        assertTrue(CountingConverter.CALLS.get() > afterFirst,
                "the second result set must go through the converter its own quirks hold");

        CountingConverter.tag = "second";
        readWith(plan, secondQuirks);
        assertTrue(CountingConverter.SEEN.contains("second"),
                "the converter is looked up per result set, so a change of what it does is seen at once");
    }

    /**
     * The quirks are not part of the shape, since they are consulted per call. Two {@code Sql2o} with different quirks
     * therefore share one compiled reader rather than compiling the same thing twice.
     */
    @Test
    public void twoSql2oWithDifferentQuirksShareOneReader() {
        writeRow();

        final RowPlanCompiler.Outcome first = outcomeFor(Pojo.class, new NoQuirks(), SELECT);
        final Map<Class, Converter> converters = new java.util.HashMap<>();
        converters.put(String.class, new CountingConverter());
        final RowPlanCompiler.Outcome second = outcomeFor(Pojo.class, new NoQuirks(converters), SELECT);

        assertTrue(first.compiled(), "not compiled: " + first.refusal());
        assertTrue(second.compiled(), "not compiled: " + second.refusal());
        assertSame(first.plan().reader(), second.plan().reader(),
                "the quirks are not part of the shape, so the reader is the same one");
    }

    /** A different set of columns is a different shape, and has to be a different reader. */
    @Test
    public void anotherSetOfColumnsIsAnotherReader() {
        writeRow();

        assertTrue(planFor(Pojo.class, SELECT) != planFor(Pojo.class, "select text from SHARED"));
    }

    /** And a different naming convention changes what a column is bound to, so it is part of the shape. */
    @Test
    public void anotherNamingConventionIsAnotherReader() {
        writeRow();

        assertTrue(planFor(Pojo.class, SELECT, sql2o.getQuirks(), false)
                        != planFor(Pojo.class, SELECT, sql2o.getQuirks(), true),
                "the naming convention is part of the key, since it decides what a column is bound to");
    }

    /**
     * The reader is shared between threads as well as between result sets, which is what lets the cache hold one instance
     * at all. A field on the reader would make this fail intermittently rather than reliably, so the columns are
     * deliberately interleaved across many rounds.
     */
    @Test
    public void oneReaderIsSafeToCallFromSeveralThreadsAtOnce() throws Exception {
        writeRow();

        final RowPlan plan = planFor(Pojo.class, SELECT);
        final int threads = 8;
        final int rounds = 40;

        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final List<Callable<String>> work = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                work.add(() -> {
                    String last = null;
                    for (int round = 0; round < rounds; round++) {
                        try (java.sql.Connection connection = DriverManager.getConnection(URL, "sa", "");
                             ResultSet rs = connection.createStatement().executeQuery(SELECT)) {
                            while (rs.next()) {
                                final Pojo pojo = (Pojo) plan.reader().build(rs, sql2o.getQuirks(),
                                        plan.convertersFor(sql2o.getQuirks()));
                                last = pojo.text + "/" + pojo.number;
                            }
                        }
                    }
                    return last;
                });
            }

            for (Future<String> done : pool.invokeAll(work)) {
                assertEquals("a row/1", done.get());
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "the pool did not finish");
        }
    }

    /**
 * Two classes of the same package, compiled from the same columns, must not end up sharing a reader.
 *
 * <p>The name of a reader carries a digest of the shape it was built for. If the class were not part of that digest, two
 * classes living in the same package would produce the same name, the second definition would collide with the first, and
 * the reader already defined — built for the other class — would be handed out for this one. That reads as values landing
 * in the wrong fields rather than as an error, which is why it is worth a test of its own.
 */
    @Test
    public void twoClassesOfOnePackageWithTheSameColumnsGetReadersOfTheirOwn() throws SQLException {
        writeRow();

        final RowPlan forPojo = planFor(Pojo.class, SELECT);
        final RowPlan forOther = planFor(OtherPojo.class, SELECT);

        assertNotSame(forPojo.reader(), forOther.reader(),
                "two classes must not be answered with one reader, however alike they look");

        // The reader compiled for OtherPojo has to build an OtherPojo, which the one compiled for Pojo could not do even
        // if it were handed over: it puts an int where this class declares a String.
        final Object built = readOne(handlerFor(forOther));
        assertEquals(OtherPojo.class, built.getClass(), "the reader built the class it was compiled for");
        assertEquals("a row", ((OtherPojo) built).text);
    }

    /**
     * A second class in this package, with the same two field names but a different type on one of them, so that being handed
     * the wrong reader is not a matter of two objects looking alike but of one of them failing outright.
     */
    public static class OtherPojo {
        public String text;
        public String number;
    }

    // ---------------------------------------------------------------- plumbing

    private RowPlanCompiler.Outcome outcomeFor(Class<?> type, Quirks quirks, String select) {
        final Settings settings = new Settings(new NamingConvention(false, false), quirks, true);
        return ReaderCache.get(type, labelsOf(select), settings, Map.of());
    }

    /**
     * The plan for a select, worked out from the labels the database actually reports rather than from the text of the
     * query, and through the cache rather than around it. Going round the cache would compile the same shape twice and
     * hand back two readers, which is the very thing the cache exists to prevent.
     */
    private RowPlan planFor(Class<?> type, String select) {
        return planFor(type, select, sql2o.getQuirks(), false);
    }

    private RowPlan planFor(Class<?> type, String select, Quirks quirks, boolean caseSensitive) {
        final String[] labels = labelsOf(select);
        final Settings settings = new Settings(new NamingConvention(caseSensitive, false), quirks, true);
        final RowPlanCompiler.Outcome outcome = ReaderCache.get(type, labels, settings, Map.of());

        assertTrue(outcome.compiled(), "not compiled: " + outcome.refusal());
        return outcome.plan();
    }

    /** The column labels a select comes back with, which is what a factory is given before a row. */
    private static String[] labelsOf(String select) {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet rs = connection.createStatement().executeQuery(select)) {
            final var meta = rs.getMetaData();
            final String[] labels = new String[meta.getColumnCount()];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = meta.getColumnLabel(i + 1);
            }
            return labels;
        } catch (SQLException e) {
            throw new AssertionError("a select over a table this test created cannot fail", e);
        }
    }

    private ResultSetHandler<Object> handlerFor(RowPlan plan) {
        return rs -> plan.reader().build(rs, sql2o.getQuirks(), plan.convertersFor(sql2o.getQuirks()));
    }

    private void readWith(RowPlan plan, Quirks quirks) {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet rs = connection.createStatement().executeQuery(SELECT)) {
            assertNotNull(plan.reader());
            while (rs.next()) {
                plan.reader().build(rs, quirks, plan.convertersFor(quirks));
            }
        } catch (SQLException e) {
            throw new AssertionError("a statement over a table this test created cannot fail", e);
        }
    }

    private void writeRow() {
        try (Connection connection = sql2o.open()) {
            try {
                connection.createQuery("drop table SHARED").executeUpdate();
            } catch (RuntimeException ignored) {
                // The table is not there, which is exactly the state this was called in.
            }
            connection.createQuery(DDL).executeUpdate();
            connection.createQuery(INSERT).executeUpdate();
        }
    }


}