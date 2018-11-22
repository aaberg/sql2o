package org.sql2o.converters;

import java.math.BigInteger;

public class BigIntegerConverter extends NumberConverter<BigInteger> {


    public BigIntegerConverter() {
        super(false);
    }

    @Override
    protected BigInteger convertNumberValue(Number number) {
        if(null == number){
            return null;
        }
        else if (number instanceof BigInteger){
            return (BigInteger)number;
        }
        else{
            return BigInteger.valueOf(number.intValue());
        }

    }

    @Override
    protected BigInteger convertStringValue(String string) {
        if(null != string) {
            return BigInteger.valueOf(Integer.parseInt(string));
        }else{
            return null;
        }
    }

    @Override
    protected String getTypeDescription() {
        return BigInteger.class.toString();
    }
}
