package org.sql2o.bytecode;

import org.sql2o.DefaultResultSetHandlerFactoryBuilder;
import org.sql2o.NamingConvention;
import org.sql2o.ResultSetHandler;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.ResultSetHandlerFactoryBuilder;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.Quirks;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The builder to hand to a query: {@code query.setResultSetHandlerFactoryBuilder(new BytecodeResultSetHandlerFactoryBuilder())}.
 *
 * <p>Not picked up by itself. Nothing in core knows this extension is there, which is deliberate — it adds a bytecode
 * library to the runtime of every project that uses it, and that is not a decision to make on someone's behalf.
 */
public class BytecodeResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {

    /** How a shape that cannot be compiled is dealt with. */
    private boolean fallbackAllowed = true;

    private final Map<String, String> columnMappings = new HashMap<>();

    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private boolean throwOnMappingError;
    private Quirks quirks;

    /**
     * Whether a shape the compiler refuses is handed to the reflective path.
     *
     * <p>Allowed by default, so that this builder is a drop-in for the one of core and a caller has nothing to notice.
     * Refused shapes are records with a column they have no component for, a dotted column name, a property with neither
     * a setter nor a field, a class whose package cannot be defined into, and a class the reflective path could not build
     * either.
     *
     * <p>Turning it off is how a caller finds out where the bytecode is not being used: every refusal then raises a
     * {@link Sql2oException} naming the reason, which is otherwise only visible in a debugger.
     */
    public boolean isFallbackAllowed() {
        return fallbackAllowed;
    }

    public void setFallbackAllowed(boolean fallbackAllowed) {
        this.fallbackAllowed = fallbackAllowed;
    }

    @Override
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean isAutoDeriveColumnNames() {
        return autoDeriveColumnNames;
    }

    @Override
    public void setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
        this.autoDeriveColumnNames = autoDeriveColumnNames;
    }

    @Override
    public boolean isThrowOnMappingError() {
        return throwOnMappingError;
    }

    @Override
    public void throwOnMappingError(boolean throwOnMappingError) {
        this.throwOnMappingError = throwOnMappingError;
    }

    @Override
    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    @Override
    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings.clear();
        if (columnMappings != null) {
            this.columnMappings.putAll(columnMappings);
        }
    }

    @Override
    public Quirks getQuirks() {
        return quirks;
    }

    @Override
    public void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

    @Override
    public <E> ResultSetHandlerFactory<E> newFactory(Class<E> clazz) {
        final Settings settings =
                new Settings(new NamingConvention(caseSensitive, autoDeriveColumnNames), quirks, throwOnMappingError);

        // Core's own builder, given the same five settings, so that a fallback is indistinguishable from what would have
        // happened without this extension rather than a reimplementation of it.
        final DefaultResultSetHandlerFactoryBuilder reflective = new DefaultResultSetHandlerFactoryBuilder();
        reflective.setCaseSensitive(caseSensitive);
        reflective.setAutoDeriveColumnNames(autoDeriveColumnNames);
        reflective.throwOnMappingError(throwOnMappingError);
        reflective.setQuirks(quirks);
        reflective.setColumnMappings(columnMappings);

        return new BytecodeResultSetHandlerFactory<>(
                clazz, settings, Map.copyOf(columnMappings), fallbackAllowed, reflective.newFactory(clazz));
    }
}