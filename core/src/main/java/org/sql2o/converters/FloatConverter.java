package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into a {@link Float}.
 */
public class FloatConverter extends NumberConverter<Float> {

    public FloatConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Float convertNumberValue(Number val) {
        return val.floatValue();
    }

    @Override
    protected Float convertStringValue(String val) {
        return Float.parseFloat(val);
    }

    @Override
    protected String getTypeDescription() {
        return Float.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, Float val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.FLOAT);
        }
        else {
            stmt.setObject(name, val);
        }
    }
}
