package org.sql2o.converters;

import java.math.BigInteger;

public class BigIntegerConverter extends NumberConverter<BigInteger> {


    public BigIntegerConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected BigInteger convertNumberValue(Number number) {
        if (number instanceof BigInteger){
            return (BigInteger)number;
        }
        else{
            return BigInteger.valueOf(number.intValue());
        }

    }

    @Override
    protected BigInteger convertStringValue(String string) {
        return BigInteger.valueOf(Integer.parseInt(string));
    }

    @Override
    protected String getTypeDescription() {
        return BigInteger.class.toString();
    }
}
