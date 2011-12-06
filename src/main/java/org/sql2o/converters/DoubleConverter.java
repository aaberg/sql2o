package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class DoubleConverter extends NumberConverter<Double> {

    public DoubleConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Double convertNumberValue(Number val) {
        return  val.doubleValue();
    }

    @Override
    protected Double convertStringValue(String val) {
        return Double.parseDouble(val);
    }

    @Override
    protected String getTypeDescription() {
        return Double.class.toString();
    }
}
