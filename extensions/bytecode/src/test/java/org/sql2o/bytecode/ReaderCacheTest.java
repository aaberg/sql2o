package org.sql2o.bytecode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.quirks.NoQuirks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the cache does with a shape, and what it is allowed to forget.
 *
 * <p>The entries are held softly, so losing one is expected rather than exceptional. That is the whole reason a cleared
 * cache has to keep working: the reader itself is defined into the class loader of the class it reads and cannot be
 * unloaded by clearing a map, so the next query of that shape finds the class already there and takes it back.
 */
public class ReaderCacheTest {

    public static class Pojo {
        public String text;
        public int number;
    }

    private static final String[] COLUMNS = {"text", "number"};

    @BeforeEach
    @AfterEach
    public void emptyTheCache() {
        ReaderCache.clear();
    }

    @Test
    public void aShapeIsCompiledOnceAndThenServedFromTheCache() {
        final RowPlanCompiler.Outcome first = get(Pojo.class, COLUMNS, false);
        final RowPlanCompiler.Outcome second = get(Pojo.class, COLUMNS, false);

        assertTrue(first.compiled(), first.refusal());
        assertSame(first.plan().reader(), second.plan().reader(), "the second call has to find the first reader");
    }

    @Test
    public void differentColumnsAreDifferentShapes() {
        assertNotSame(get(Pojo.class, COLUMNS, false).plan().reader(),
                get(Pojo.class, new String[]{"text"}, false).plan().reader());
    }

    @Test
    public void differentNamingConventionsAreDifferentShapes() {
        assertNotSame(get(Pojo.class, COLUMNS, false).plan().reader(),
                get(Pojo.class, COLUMNS, true).plan().reader());
    }

    @Test
    public void whetherAnUnmappedColumnIsAnErrorIsPartOfTheShape() {
        assertNotSame(get(Pojo.class, COLUMNS, false).plan().reader(),
                get(Pojo.class, COLUMNS, true).plan().reader());
    }

    @Test
    public void theColumnMappingsArePartOfTheShape() {
        final Settings settings = settings(false, true);

        assertNotSame(ReaderCache.get(Pojo.class, COLUMNS, settings, Map.of("text", "number")).plan().reader(),
                ReaderCache.get(Pojo.class, COLUMNS, settings, Map.of("number", "text")).plan().reader());
    }

    /**
     * What a cleared cache has to survive. The reader is defined into the class loader of the class it reads, so it is
     * still there after the entry that remembered it is gone, and the shape has to be served rather than refused.
     */
    @Test
    public void aShapeStillWorksAfterTheCacheHasBeenForgotten() {
        final RowPlan before = get(Pojo.class, COLUMNS, false).plan();

        ReaderCache.clear();
        assertEquals(0, ReaderCache.size(), "the cache is empty after being cleared");

        final RowPlan after = get(Pojo.class, COLUMNS, false).plan();
        assertNotNull(after.reader(), "the shape has to be compiled again rather than refused");
        assertSame(before.reader().getClass(), after.reader().getClass(),
                "the class is still defined in the loader, so the same one comes back");
    }

    @Test
    public void theCacheCountsTheShapesItHolds() {
        assertEquals(0, ReaderCache.size());

        get(Pojo.class, COLUMNS, false);
        assertEquals(1, ReaderCache.size());

        get(Pojo.class, COLUMNS, false);
        assertEquals(1, ReaderCache.size(), "the same shape does not take a second entry");

        get(Pojo.class, new String[]{"text"}, false);
        assertEquals(2, ReaderCache.size());
    }

    /** A refusal is remembered like a plan, so a shape that cannot be compiled is not recompiled on every query. */
    @Test
    public void aRefusedShapeIsRememberedAsARefusal() {
        final RowPlanCompiler.Outcome first = get(ReadOnlyProperty.class, new String[]{"computed"}, false);
        final RowPlanCompiler.Outcome second = get(ReadOnlyProperty.class, new String[]{"computed"}, false);

        assertTrue(!first.compiled(), "a property with nothing to write through is not compiled");
        assertEquals(1, ReaderCache.size(), "the refusal is an entry like a plan is");
        assertSame(first.refusal(), second.refusal(), "the second call finds the refusal rather than recompiling");
    }

    /**
     * What losing an entry to the garbage collector has to look like. The entries are soft, so this happens without
     * anyone calling clear; emptying every referent by hand is the deterministic version of it.
     */
    @Test
    public void aShapeWhoseEntryWasCollectedIsCompiledAgain() throws ReflectiveOperationException {
        final Class<?> defined = get(Pojo.class, COLUMNS, false).plan().reader().getClass();
        collectEveryEntry();

        final RowPlanCompiler.Outcome again = get(Pojo.class, COLUMNS, false);

        assertTrue(again.compiled(), "a collected entry is recompiled rather than reported missing");
        assertSame(defined, again.plan().reader().getClass(),
                "the class is still defined in the loader, so the same one comes back");
    }

    private static void collectEveryEntry() throws ReflectiveOperationException {
        final var plans = ReaderCache.class.getDeclaredField("PLANS");
        plans.setAccessible(true);
        for (Object reference : ((Map<?, ?>) plans.get(null)).values()) {
            ((java.lang.ref.SoftReference<?>) reference).clear();
        }
    }
    public static class ReadOnlyProperty {
        public String getComputed() {
            return "computed";
        }
    }

    private Settings settings(boolean caseSensitive, boolean throwOnMappingError) {
        return new Settings(new NamingConvention(caseSensitive, false), new NoQuirks(), throwOnMappingError);
    }

    private RowPlanCompiler.Outcome get(Class<?> type, String[] columns, boolean caseSensitive) {
        return ReaderCache.get(type, columns, settings(caseSensitive, true), Map.of());
    }
}