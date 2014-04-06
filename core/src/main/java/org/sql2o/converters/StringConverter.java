package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Used by sql2o to convert a value from the database into a {@link String}.
 */
public class StringConverter extends BuiltInConverterBase<String> {

    public String convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (val instanceof Clob) {
            Clob clobVal = (Clob)val;
            try {
                return clobVal.getSubString(1, (int)clobVal.length());
            } catch (SQLException e) {
                throw new ConverterException("error converting clob to String", e);
            }
        }

        return val.toString().trim();
    }

    public void addParameter(StatementParameterSetter stmt, String name, String val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.VARCHAR);
        }
        else {
            stmt.setString(name, val);
        }
    }
}
