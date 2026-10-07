package org.sql2o.bytecode;

import org.sql2o.ResultSetHandler;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.Quirks;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * Builds one object out of a row, by bytecode where it can and by the reflective path where it cannot.
 *
 * <p>The shape is worked out once per result set, from the column names the quirks report, which is also where the
 * reflective path gets them from. If a reader exists for the shape the handler is a forwarder; if not, the handler of
 * core answers instead and nothing about the mapping changes.
 *
 * <p>The fallback is not a degraded mode. A reader is only compiled for a shape it can produce exactly what the
 * reflective path would, so a caller cannot tell which of the two answered — the same values, the same errors worded the
 * same way — and the only way to notice is to turn the fallback off.
 */
public class BytecodeResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {

    private final Class<T> targetClass;
    private final Settings settings;
    private final Map<String, String> columnMappings;
    private final boolean fallbackAllowed;
    private final ResultSetHandlerFactory<T> fallback;

    /**
     * @param columnMappings copied by the builder, because a query hands out its own mutable map and a key that changed
     *                       under the cache would compile one shape and read another
     * @param fallback        the handler of core, built by the builder through core's own builder so that the settings
     *                        reach it by the route they would take without this extension
     */
    BytecodeResultSetHandlerFactory(Class<T> targetClass, Settings settings, Map<String, String> columnMappings,
                                    boolean fallbackAllowed, ResultSetHandlerFactory<T> fallback) {
        this.targetClass = targetClass;
        this.settings = settings;
        this.columnMappings = columnMappings;
        this.fallbackAllowed = fallbackAllowed;
        this.fallback = fallback;
    }

    @Override
    public ResultSetHandler<T> newResultSetHandler(ResultSetMetaData meta) throws SQLException {
        final Quirks quirks = settings.getQuirks();

        // Read once per result set, and from the quirks rather than the metadata directly, for the same reason the
        // reflective path does it that way: the label a query gave a column is what maps to a property, and for some
        // drivers that is not the name underneath.
        final int columnCount = meta.getColumnCount();
        final String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = quirks.getColumnName(meta, i + 1);
        }

        final RowPlanCompiler.Outcome outcome = ReaderCache.get(targetClass, columnNames, settings, columnMappings);
        if (outcome.compiled()) {
            return handlerFor(outcome.plan(), quirks);
        }

        if (!fallbackAllowed) {
            throw new Sql2oException("Not compiled for " + targetClass.getName() + ": " + outcome.refusal()
                    + ". Turn setFallbackAllowed on to have it mapped the reflective way instead.");
        }
        return fallback.newResultSetHandler(meta);
    }

    /**
     * The cast is the one place where the type parameter cannot be proven, and it is safe because the plan was compiled
     * from this very class: its reader is emitted with a {@code new} of it and returns nothing else. The generated code is
     * erased to {@code Object}, which is all that is left of it at runtime.
     */
    @SuppressWarnings("unchecked")
    private ResultSetHandler<T> handlerFor(RowPlan plan, Quirks quirks) {
        return (ResultSetHandler<T>) new BytecodeResultSetHandler(plan, quirks);
    }
}