package org.sql2o.reflect;

import junit.framework.TestCase;
import org.sql2o.reflection.FieldSetterFactory;
import org.sql2o.reflection.ReflectionFieldSetterFactory;

/**
 * User: dimzon
 * Date: 4/9/14
 * Time: 9:44 PM
 */
public class ReflectionFieldSetterFactoryTest extends AbstractFieldSetterFactoryTest {
    public ReflectionFieldSetterFactoryTest() {
        super(new ReflectionFieldSetterFactory());
    }
}
