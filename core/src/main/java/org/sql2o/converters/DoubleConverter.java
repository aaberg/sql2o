package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into a  {@link Double}.
 */
public class DoubleConverter extends NumberConverter<Double> {

    public DoubleConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Double convertNumberValue(Number val) {
        return  val.doubleValue();
    }

    @Override
    protected Double convertStringValue(String val) {
        return Double.parseDouble(val);
    }

    @Override
    protected String getTypeDescription() {
        return Double.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, Double val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.DOUBLE);
        }
        else {
            stmt.setObject(name, val);
        }
    }
}
