package org.sql2o.bytecode;

import org.sql2o.ResultSetHandler;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.Quirks;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The per result set half of the mapping, and the reason it is so small.
 *
 * <p>Everything that could be decided when the shape was compiled is in the plan already, so all this has to do is hold
 * the quirks of this result set, resolve the converters for its columns once, and forward each row to the reader. That
 * is why a converter registered after the plan was compiled is still used here: the resolution happens per result set,
 * not per plan.
 *
 * <p>One of these per result set and never shared, which is what lets the reader behind it be shared between all of
 * them.
 */
final class BytecodeResultSetHandler implements ResultSetHandler<Object> {

    private final RowPlan plan;
    private final Quirks quirks;
    private final Converter<?>[] converters;

    BytecodeResultSetHandler(RowPlan plan, Quirks quirks) {
        this.plan = plan;
        this.quirks = quirks;
        this.converters = plan.convertersFor(quirks);
    }

    @Override
    public Object handle(ResultSet resultSet) throws SQLException {
        return plan.reader().build(resultSet, quirks, converters);
    }
}