package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class LongConverter extends NumberConverter<Long>{

    @Override
    protected Long convertNumberValue(Number val) {
        return val.longValue();
    }

    @Override
    protected Long convertStringValue(String val) {
        return Long.parseLong(val);
    }

    @Override
    protected String getTypeDescription() {
        return Long.class.toString();
    }
}
