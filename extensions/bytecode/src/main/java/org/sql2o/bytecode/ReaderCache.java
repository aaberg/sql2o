package org.sql2o.bytecode;

import org.sql2o.NamingConvention;
import org.sql2o.Settings;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The compiled readers, one per shape.
 *
 * <p>A shape is everything that decides what a column is bound to: the target class, the names of the columns, the naming
 * convention, the column mappings, and whether an unmapped column is an error. The quirks are deliberately not part of
 * it — they are consulted on every call — so one entry serves every {@code Sql2o} instance in the jvm.
 *
 * <p>The values are held softly on purpose. A reader is defined into the class loader of the class it reads, so a strong
 * reference here would keep that loader, and everything it loaded, alive for as long as this map is. Losing an entry
 * costs a recompilation and nothing else, which is the trade this takes.
 */
public final class ReaderCache {

    private record ShapeKey(
            Class<?> targetClass,
            List<String> columnNames,
            NamingConvention namingConvention,
            Map<String, String> columnMappings,
            boolean throwOnMappingError) {
    }

    private static final ConcurrentMap<ShapeKey, SoftReference<RowPlanCompiler.Outcome>> PLANS = new ConcurrentHashMap<>();

    private ReaderCache() {
    }

    /**
     * The reader for a shape, compiling it the first time it is asked for.
     *
     * @return the plan, or the reason there is none. A refusal is remembered like a plan, so a shape that cannot be
     * compiled is not recompiled on every query: the check itself only reads caches, but there is no reason to pay even
     * that more than once.
     */
    static RowPlanCompiler.Outcome get(Class<?> targetClass, String[] columnNames, Settings settings,
                                       Map<String, String> columnMappings) {

        final ShapeKey key = new ShapeKey(
                targetClass,
                List.of(columnNames),
                settings.getNamingConvention(),
                columnMappings,
                settings.isThrowOnMappingError());

        final SoftReference<RowPlanCompiler.Outcome> cached = PLANS.get(key);
        if (cached != null) {
            final RowPlanCompiler.Outcome remembered = cached.get();
            if (remembered != null) {
                // The bytes are only ever read by a test, straight after compiling, so a plan that comes out of the
                // cache carries no copy of them.
                return remembered.compiled() ? RowPlanCompiler.Outcome.compiled(remembered.plan(), null) : remembered;
            }
        }

        final RowPlanCompiler.Outcome outcome =
                new RowPlanCompiler(targetClass, settings, columnNames).compile(columnMappings);
        PLANS.put(key, new SoftReference<>(outcome));
        return outcome;
    }

    /**
     * Forgets every compiled reader.
     *
     * <p>The classes themselves are defined into their own class loaders and are not unloadable by this, so what is
     * actually given up here is the memory of the plans and the ability to reuse the readers. After a clear the next
     * query of a shape finds its reader already defined and takes it back rather than defining a second one.
     */
    public static void clear() {
        PLANS.clear();
    }

    /** How many shapes are cached, which is what a caller wants to know before suspecting a leak. */
    public static int size() {
        return PLANS.size();
    }
}