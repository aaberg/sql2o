package org.sql2o.converters;

import org.sql2o.Sql2oException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class IntegerConverter extends NumberConverter<Integer>{

    public IntegerConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Integer convertNumberValue(Number val) {
        return val.intValue();
    }

    @Override
    protected Integer convertStringValue(String val) {
        return Integer.parseInt(val);
    }

    @Override
    protected String getTypeDescription() {
        return Integer.class.toString();
    }
}
