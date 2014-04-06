package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into an {@link Integer}.
 */
public class IntegerConverter extends NumberConverter<Integer>{

    public IntegerConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Integer convertNumberValue(Number val) {
        return val.intValue();
    }

    @Override
    protected Integer convertStringValue(String val) {
        return Integer.parseInt(val);
    }

    @Override
    protected String getTypeDescription() {
        return Integer.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, Integer val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.INTEGER);
        }
        else {
            stmt.setInt(name, val);
        }
    }
}
