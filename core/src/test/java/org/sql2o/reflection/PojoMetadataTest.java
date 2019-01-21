package org.sql2o.reflection;

import javassist.*;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests the {@link PojoMetadata} class.
 * @author mavi
 */
public class PojoMetadataTest {
    /**
     * Tests that when there are two getters with different return types, the one with more concrete type is preferred.
     * See https://github.com/aaberg/sql2o/issues/314 for more details.
     */
    @Test
    public void testPrefersGetterWithMoreConcreteType() throws Exception {
        Class<?> clazz = createClassWithGetters(false);
        if (clazz.getDeclaredMethods()[0].getReturnType() == Object.class) {
            // the method order is undefined :( We need to have two methods, first one returning String, second one
            // returning Object. The old PojoMetadata would incorrectly use the latter; the fixed version would use the
            // function returning more concrete result.
            //
            // try to create the class with reversed method ordering, maybe it helps?
            clazz = createClassWithGetters(true);
            if (clazz.getDeclaredMethods()[0].getReturnType() == Object.class) {
                // nah, didn't help. bail out.
                throw new AssumptionViolatedException("Can't enforce method order");
            }
        }

        final PojoMetadata metadata = new PojoMetadata(clazz, false, false, new HashMap<String, String>(), true);
        assertEquals(String.class, metadata.getPropertyGetter("id").getType());
    }

    /**
     * Tests that when there are two setters with different return types, the one with more concrete type is preferred.
     * See https://github.com/aaberg/sql2o/issues/314 for more details.
     */
    @Test
    public void testPrefersSetterWithMoreConcreteType() throws Exception {
        Class<?> clazz = createClassWithSetters(false);
        if (clazz.getDeclaredMethods()[0].getParameterTypes()[0] == Object.class) {
            // the method order is undefined :( We need to have two methods, first one returning String, second one
            // returning Object. The old PojoMetadata would incorrectly use the latter; the fixed version would use the
            // function returning more concrete result.
            //
            // try to create the class with reversed method ordering, maybe it helps?
            clazz = createClassWithSetters(true);
            if (clazz.getDeclaredMethods()[0].getParameterTypes()[0] == Object.class) {
                // nah, didn't help. bail out.
                throw new AssumptionViolatedException("Can't enforce method order");
            }
        }

        final PojoMetadata metadata = new PojoMetadata(clazz, false, false, new HashMap<String, String>(), true);
        assertEquals(String.class, metadata.getPropertySetter("id").getType());
    }

    private Class<?> createClassWithGetters(boolean objectThenString) throws CannotCompileException, NotFoundException {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass ctClass = pool.makeClass("my.test.Klass" + UUID.randomUUID().toString().replace("-", ""));
        ctClass.addField(new CtField(pool.get("java.lang.String"), "id", ctClass));
        if (objectThenString) {
            ctClass.addMethod(CtNewMethod.make("public Object getId() { return id; }", ctClass));
            ctClass.addMethod(CtNewMethod.make("public String getId() { return id; }", ctClass));
        } else {
            ctClass.addMethod(CtNewMethod.make("public String getId() { return id; }", ctClass));
            ctClass.addMethod(CtNewMethod.make("public Object getId() { return id; }", ctClass));
        }
        final Class<?> clazz = ctClass.toClass();
        assertEquals(2, clazz.getDeclaredMethods().length);
        return clazz;
    }

    private Class<?> createClassWithSetters(boolean objectThenString) throws CannotCompileException, NotFoundException {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass ctClass = pool.makeClass("my.test.Klass" + UUID.randomUUID().toString().replace("-", ""));
        ctClass.addField(new CtField(pool.get("java.lang.Object"), "id", ctClass));
        if (objectThenString) {
            ctClass.addMethod(CtNewMethod.make("public void setId(Object id) { this.id = id; }", ctClass));
            ctClass.addMethod(CtNewMethod.make("public void setId(String id) { this.id = id; }", ctClass));
        } else {
            ctClass.addMethod(CtNewMethod.make("public void setId(String id) { this.id = id; }", ctClass));
            ctClass.addMethod(CtNewMethod.make("public void setId(Object id) { this.id = id; }", ctClass));
        }
        final Class<?> clazz = ctClass.toClass();
        assertEquals(2, clazz.getDeclaredMethods().length);
        return clazz;
    }
}
