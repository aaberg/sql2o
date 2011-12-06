package org.sql2o.converters;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/15/11
 * Time: 10:09 AM
 * To change this template use File | Settings | File Templates.
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
}
