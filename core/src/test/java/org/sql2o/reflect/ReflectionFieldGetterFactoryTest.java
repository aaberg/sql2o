package org.sql2o.reflect;

import org.sql2o.reflection.ReflectionFieldGetterFactory;

/**
 * @author mdelapenya
 */
public class ReflectionFieldGetterFactoryTest extends AbstractFieldGetterFactoryTest {
    public ReflectionFieldGetterFactoryTest() {
        super(new ReflectionFieldGetterFactory());
    }
}