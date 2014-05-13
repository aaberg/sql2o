package org.sql2o.reflect;

import org.sql2o.reflection.MethodAccessorsGenerator;

/**
 * User: dimzon
 * Date: 5/13/14
 * Time: 3:45 AM
 */
public class MethodAccessorsGeneratorObjectConstructorFactoryTest extends AbstractObjectConstructorFactoryTest {
    public class POJO2{
        public boolean constructorInvoked = true;
        public POJO2() {
           fail("Constructor can't be called");
        }
        public Object parent(){
            return MethodAccessorsGeneratorObjectConstructorFactoryTest.this;
        }
    }
    public static class POJO3{
        public boolean constructorInvoked = true;
    }

    public MethodAccessorsGeneratorObjectConstructorFactoryTest() {
        super(new MethodAccessorsGenerator());
    }

    public void testCantCallConstructor(){
        POJO2 pojo2 = (POJO2) ocf.newConstructor(POJO2.class).newInstance();
        assertNotNull(pojo2);
        assertNull(pojo2.parent());
        assertFalse(pojo2.constructorInvoked);
    }
    public void testCallConstructor(){
        POJO3 pojo3 = (POJO3) ocf.newConstructor(POJO3.class).newInstance();
        assertNotNull(pojo3);
        assertTrue(pojo3.constructorInvoked);
    }
}
