package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ByteConverter extends NumberConverter<Byte> {

    public ByteConverter(boolean primitive) {
        super(primitive);
    }

    @Override
    protected Byte convertNumberValue(Number val) {
        return val.byteValue();
    }

    @Override
    protected Byte convertStringValue(String val) {
        return Byte.parseByte(val);
    }

    @Override
    protected String getTypeDescription() {
        return Byte.class.toString();
    }
}
