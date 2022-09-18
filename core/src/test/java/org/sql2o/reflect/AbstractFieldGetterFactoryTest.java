package org.sql2o.reflect;

import junit.framework.TestCase;
import org.sql2o.reflection.FieldGetterFactory;
import org.sql2o.reflection.Getter;

import java.lang.reflect.Field;

/**
 * @author mdelapenya
 */
public abstract class AbstractFieldGetterFactoryTest extends TestCase {
    public static class POJO1{
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

    public void testAllTypes() throws IllegalAccessException {
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

        Field[] fields = pojo.getClass().getDeclaredFields();

        for (Field field : fields) {
            Getter getter = fgf.newGetter(field);
            assertSame(field.getType(),getter.getType());

            Object val1 = field.get(pojo);
            assertEquals(val1, getter.getProperty(pojo));
        }
    }

    public final FieldGetterFactory fgf;

    protected AbstractFieldGetterFactoryTest(FieldGetterFactory fgf) {
        this.fgf = fgf;
    }
}