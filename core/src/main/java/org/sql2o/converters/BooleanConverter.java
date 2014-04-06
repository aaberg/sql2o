package org.sql2o.converters;

import org.sql2o.SqlParameter;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 6/1/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class BooleanConverter extends BuiltInConverterBase<Boolean> {

    public Boolean convert(Object val) throws ConverterException {
        if (val == null) return null;

        if (Boolean.class.isAssignableFrom(val.getClass())) {
            return (Boolean) val;
        }

        if (Number.class.isAssignableFrom(val.getClass())) {
            return ((Number)val).intValue() > 0;
        }

        if (String.class.isAssignableFrom(val.getClass())) {
            String strVal = ((String)val).trim();
            return "Y".equalsIgnoreCase(strVal) || "YES".equalsIgnoreCase(strVal) || "TRUE".equalsIgnoreCase(strVal) ||
                    "T".equalsIgnoreCase(strVal) || "J".equalsIgnoreCase(strVal);
        }

        throw new ConverterException("Don't know how to convert type " + val.getClass().getName() + " to " + Boolean.class.getName());
    }

    public void addParameter(StatementParameterSetter stmt, String name, Boolean val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.BOOLEAN);
        }
        else {
            stmt.setObject(name, val);
        }
    }
}
