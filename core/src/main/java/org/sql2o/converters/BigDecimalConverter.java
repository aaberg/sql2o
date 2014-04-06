package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Used by sql2o to convert a value from the database into a {@link BigDecimal}.
 */
public class BigDecimalConverter extends NumberConverter<BigDecimal>{

    public BigDecimalConverter() {
        super(false);
    }

    @Override
    protected BigDecimal convertNumberValue(Number val) {
        if (val.getClass().equals(BigDecimal.class)){
            return (BigDecimal)val;
        }
        else{
            return BigDecimal.valueOf(val.doubleValue());
        }
    }

    @Override
    protected BigDecimal convertStringValue(String val) {
        return BigDecimal.valueOf(Double.parseDouble(val));
    }

    @Override
    protected String getTypeDescription() {
        return BigDecimal.class.toString();
    }

    public void addParameter(StatementParameterSetter stmt, String name, BigDecimal val) throws SQLException {
        stmt.setObject(name, val);
    }
}
