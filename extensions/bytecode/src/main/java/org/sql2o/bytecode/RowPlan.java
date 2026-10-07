package org.sql2o.bytecode;

import org.sql2o.converters.Converter;
import org.sql2o.quirks.Quirks;

import java.util.Arrays;

/**
 * A compiled shape: one reader instance plus what is needed to feed it.
 *
 * <p>Shared between every query of the same shape, which is the point of compiling at all, so nothing here may depend on
 * a single result set. The reader is stateless and the converters are left out on purpose: they are resolved per result
 * set, so a converter registered after this plan was compiled is still picked up, and so one plan serves every
 * {@link Quirks} rather than only the one that happened to be current when it was compiled.
 */
public final class RowPlan {

    private final RowReader reader;

    /** One entry per column of the result set, with null where the column is compiled away. */
    private final Class<?>[] targetTypes;

    /** One entry per column: where its value is going, worded the way the reflective path words it. */
    private final String[] descriptions;

    RowPlan(RowReader reader, Class<?>[] targetTypes, String[] descriptions) {
        if (targetTypes.length != descriptions.length) {
            throw new IllegalArgumentException("a column has one type and one description or neither");
        }
        this.reader = reader;
        this.targetTypes = targetTypes;
        this.descriptions = descriptions;
    }

    RowReader reader() {
        return reader;
    }

    /**
     * The converters for one result set, one per column.
     *
     * <p>Resolved here rather than baked into the generated code, which is what lets the generated code be stateless and
     * lets a plan outlive the quirks it was compiled under. An entry is null for a column that was compiled away, and
     * there is exactly one reason for that.
     */
    Converter<?>[] convertersFor(Quirks quirks) {
        Converter<?>[] converters = new Converter<?>[targetTypes.length];
        for (int i = 0; i < targetTypes.length; i++) {
            if (targetTypes[i] != null) {
                converters[i] = quirks.converterOf(targetTypes[i]);
            }
        }
        return converters;
    }

    /** For tests and for a message about what a plan does: one entry per column, with null where it was compiled away. */
    Class<?>[] targetTypes() {
        return targetTypes.clone();
    }

    /** For tests: the descriptions, which are what the generated code reports a conversion failure against. */
    String[] descriptions() {
        return descriptions.clone();
    }

    @Override
    public String toString() {
        return "RowPlan[" + reader.getClass().getName() + ", types=" + Arrays.toString(targetTypes) + "]";
    }
}