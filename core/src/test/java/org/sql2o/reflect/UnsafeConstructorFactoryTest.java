package org.sql2o.reflect;

import org.sql2o.reflection.UnsafeFieldSetterFactory;

/**
 * User: dimzon
 * Date: 4/9/14
 * Time: 10:16 PM
 */
public class UnsafeConstructorFactoryTest extends AbstractObjectConstructorFactoryTest {
    public UnsafeConstructorFactoryTest() {
        super(new UnsafeFieldSetterFactory());
    }
}
