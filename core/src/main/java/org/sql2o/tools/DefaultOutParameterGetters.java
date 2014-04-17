package org.sql2o.tools;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lars on 16.04.14.
 */
public class DefaultOutParameterGetters {

    public Map<Class, OutParameterGetter> map(){
        Map<Class, OutParameterGetter> map = new HashMap<Class, OutParameterGetter>();

        map.put(boolean.class, new BooleanOutParameterGetter());
        map.put(Boolean.class, new BooleanOutParameterGetter());

        map.put(byte.class, new ByteOutParameterGetter());
        map.put(Byte.class, new ByteOutParameterGetter());

        map.put(short.class, new ShortOutParameterGetter());
        map.put(Short.class, new ShortOutParameterGetter());

        map.put(int.class, new IntegerOutParameterGetter());
        map.put(Integer.class, new IntegerOutParameterGetter());

        map.put(long.class, new LongOutParameterGetter());
        map.put(Long.class, new LongOutParameterGetter());

        map.put(float.class, new FloatOutParameterGetter());
        map.put(Float.class, new FloatOutParameterGetter());

        map.put(double.class, new DoubleOutParameterGetter());
        map.put(Double.class, new DoubleOutParameterGetter());

        map.put(BigDecimal.class, new BigDecimalOutParameterGetter());

        map.put(String.class, new StringOutParameterGetter());

        map.put(Date.class, new DateOutParameterGetter());

        map.put(byte[].class, new ByteArrayOutParameterGetter());

        return map;
    }

    private class BooleanOutParameterGetter implements OutParameterGetter<Boolean> {

        public Boolean handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getBoolean(paramIdx);
        }
    }

    private class ByteOutParameterGetter implements OutParameterGetter<Byte> {

        public Byte handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getByte(paramIdx);
        }
    }
    
    private class ShortOutParameterGetter implements OutParameterGetter<Short> {

        public Short handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getShort(paramIdx);
        }
    }

    private class IntegerOutParameterGetter implements OutParameterGetter<Integer> {

        public Integer handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getInt(paramIdx);
        }
    }

    private class LongOutParameterGetter implements  OutParameterGetter<Long> {

        public Long handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getLong(paramIdx);
        }
    }

    private class FloatOutParameterGetter implements OutParameterGetter<Float> {

        public Float handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getFloat(paramIdx);
        }
    }

    private class DoubleOutParameterGetter implements OutParameterGetter<Double> {

        public Double handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getDouble(paramIdx);
        }
    }

    private class BigDecimalOutParameterGetter implements OutParameterGetter<BigDecimal> {

        public BigDecimal handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getBigDecimal(paramIdx);
        }
    }

    private class StringOutParameterGetter implements OutParameterGetter<String> {

        public String handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getString(paramIdx);
        }
    }

    private class DateOutParameterGetter implements OutParameterGetter<Date> {

        public Date handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getDate(paramIdx);
        }
    }

    private class ByteArrayOutParameterGetter implements OutParameterGetter<byte[]> {

        public byte[] handle(CallableStatement statement, int paramIdx) throws SQLException {
            return statement.getBytes(paramIdx);
        }
    }


}
