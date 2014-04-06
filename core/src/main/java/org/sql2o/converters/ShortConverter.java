package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into a {@link Short}.
 */
public class ShortConverter extends NumberConverter<Short> {

    public ShortConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Short convertNumberValue(Number val) {
        return val.shortValue();
    }

    @Override
    protected Short convertStringValue(String val) {
        return Short.parseShort(val);
    }

    @Override
    protected String getTypeDescription() {
        return Short.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, Short val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.SMALLINT);
        }
        else {
            stmt.setInt(name, val);
        }
    }
}
