package org.sql2o.reflect;

import org.sql2o.reflection.UnsafeFieldGetterFactory;

/**
 * @author mdelapenya
 */
public class UnsafeFieldGetterFactoryTest extends AbstractFieldGetterFactoryTest {
    public UnsafeFieldGetterFactoryTest() {
        super(new UnsafeFieldGetterFactory());
    }
}