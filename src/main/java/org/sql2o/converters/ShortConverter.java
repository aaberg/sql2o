package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShortConverter extends NumberConverter<Short> {

    @Override
    protected Short convertNumberValue(Number val) {
        return val.shortValue();
    }

    @Override
    protected Short convertStringValue(String val) {
        return Short.parseShort(val);
    }

    @Override
    protected String getTypeDescription() {
        return Short.class.toString();
    }
}
