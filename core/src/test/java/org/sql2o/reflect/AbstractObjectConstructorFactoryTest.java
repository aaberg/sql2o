package org.sql2o.reflect;

import junit.framework.TestCase;
import org.sql2o.reflection.ObjectConstructorFactory;

/**
 * User: dimzon
 * Date: 4/9/14
 * Time: 10:09 PM
 */
public abstract class AbstractObjectConstructorFactoryTest extends TestCase {
    // just a class
    public static class POJO1{

    }

    public final ObjectConstructorFactory ocf;
    public AbstractObjectConstructorFactoryTest(ObjectConstructorFactory ocf) {
        this.ocf = ocf;
    }

    public void testCreate(){
        Object o = ocf.newConstructor(POJO1.class).newInstance();
        assertNotNull(o);
        assertSame(POJO1.class,o.getClass());
    }


}
