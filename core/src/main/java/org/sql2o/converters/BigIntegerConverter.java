package org.sql2o.converters;

import java.math.BigInteger;

/**
 * Used by sql2o to convert a value from the database into a {@link BigInteger}.
 */
public class BigIntegerConverter extends NumberConverter<BigInteger>{

    public BigIntegerConverter() {
        super(false);
    }

    @Override
    protected BigInteger convertNumberValue(Number val) {
        if (val instanceof BigInteger){
            return (BigInteger) val;
        }
        else if (val instanceof BigDecimal){
            return val.toBigInteger();
        }
        else{
            return BigInteger.valueOf(val.longValue());
        }
    }

    @Override
    protected BigInteger convertStringValue(String val) {
        return new BigInteger(val);
    }

    @Override
    protected String getTypeDescription() {
        return BigInteger.class.toString();
    }
}
