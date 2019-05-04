package org.sql2o.converters;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Used by sql2o to convert a value from the database into a {@link BigDecimal}.
 */
public class BigDecimalConverter extends NumberConverter<BigDecimal> {
    public BigDecimalConverter() {
        super(false);
    }

    @Override
    protected BigDecimal convertNumberValue(Number val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        
        if (val instanceof BigInteger) {
            return new BigDecimal((BigInteger) val);
        }
        
        return new BigDecimal(val.doubleValue());
    }

    @Override
    protected BigDecimal convertStringValue(String val) {
        return new BigDecimal(val);
    }

    @Override
    protected String getTypeDescription() {
        return BigDecimal.class.toString();
    }
}
