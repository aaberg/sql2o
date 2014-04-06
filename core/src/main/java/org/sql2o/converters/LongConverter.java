package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into a {@link Long}.
 */
public class LongConverter extends NumberConverter<Long> {

    public LongConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Long convertNumberValue(Number val) {
        return val.longValue();
    }

    @Override
    protected Long convertStringValue(String val) {
        return Long.parseLong(val);
    }

    @Override
    protected String getTypeDescription() {
        return Long.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, Long val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.BIGINT);
        }
        else {
            stmt.setLong(name, val);
        }
    }
}
