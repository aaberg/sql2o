package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FloatConverter extends NumberConverter<Float> {

    public FloatConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Float convertNumberValue(Number val) {
        return val.floatValue();
    }

    @Override
    protected Float convertStringValue(String val) {
        return Float.parseFloat(val);
    }

    @Override
    protected String getTypeDescription() {
        return Float.class.toString();
    }
}
