package org.sql2o.reflect;

import junit.framework.TestCase;

import org.sql2o.reflection.Getter;
import org.sql2o.reflection.MethodGetterFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author mdelapenya
 */
public abstract class AbstractMethodGetterFactoryTest extends TestCase {
    public static class POJO1{
        public boolean get_boolean() {
            return this._boolean;
        }

        public byte set_byte() {
            return this._byte;
        }

        public short set_short() {
            return this._short;
        }

        public int set_int() {
            return this._int;
        }

        public long set_long() {
            return this._long;
        }

        public float set_float() {
            return this._float;
        }

        public double set_double() {
            return this._double;
        }

        public char set_char() {
            return this._char;
        }

        public Object set_obj() {
            return this._obj;
        }

        boolean _boolean;
        byte _byte;
        short _short;
        int _int;
        long _long;
        float _float;
        double _double;
        char _char;
        Object _obj;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            POJO1 pojo1 = (POJO1) o;

            if (_boolean != pojo1._boolean) return false;
            if (_byte != pojo1._byte) return false;
            if (_char != pojo1._char) return false;
            if (Double.compare(pojo1._double, _double) != 0) return false;
            if (Float.compare(pojo1._float, _float) != 0) return false;
            if (_int != pojo1._int) return false;
            if (_long != pojo1._long) return false;
            if (_short != pojo1._short) return false;
            if (_obj != null ? !_obj.equals(pojo1._obj) : pojo1._obj != null) return false;

            return true;
        }
    }


    public void testAllTypes() throws IllegalAccessException, NoSuchFieldException {
        POJO1 pojo = new POJO1();
        pojo._boolean = true;
        pojo._byte = 17;
        pojo._short=87;
        pojo._int= Integer.MIN_VALUE;
        pojo._long= 1337;
        pojo._char='a';
        pojo._double=Math.PI;
        pojo._float= (float) Math.log(93);
        pojo._obj = pojo;

        Method[] methods = pojo.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if(!method.getName().startsWith("get_")) continue;

            Field field = pojo.getClass()
                    .getDeclaredField(method.getName().substring(3));

            Getter getter = mgf.newGetter(method);
            assertSame(field.getType(),getter.getType());

            Object val1 = field.get(pojo);
            assertEquals(val1, getter.getProperty(pojo));
        }
    }


    public  final MethodGetterFactory mgf;

    public AbstractMethodGetterFactoryTest(MethodGetterFactory mgf) {
        this.mgf = mgf;
    }
}