package org.sql2o.reflection;

import javassist.*;
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
        Class<?> clazz = createClassWithGetters();
        final PojoMetadata metadata = new PojoMetadata(clazz, false, false, new HashMap<String, String>(), true);
        assertEquals(String.class, metadata.getPropertyGetter("id").getType());
    }

    /**
     * Tests that when there are two setters with different return types, the one with more concrete type is preferred.
     * See https://github.com/aaberg/sql2o/issues/314 for more details.
     */
    @Test
    public void testPrefersSetterWithMoreConcreteType() throws Exception {
        Class<?> clazz = createClassWithSetters();
        final PojoMetadata metadata = new PojoMetadata(clazz, false, false, new HashMap<String, String>(), true);
        assertEquals(String.class, metadata.getPropertySetter("id").getType());
    }

    private Class<?> createClassWithGetters() throws CannotCompileException, NotFoundException {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass ctClass = pool.makeClass("my.test.Klass" + UUID.randomUUID().toString().replace("-", ""));
        ctClass.addField(new CtField(pool.get("java.lang.String"), "id", ctClass));
        ctClass.addMethod(withBridge(CtNewMethod.make("public Object getId() { return id; }", ctClass)));
        ctClass.addMethod(CtNewMethod.make("public String getId() { return id; }", ctClass));
        final Class<?> clazz = ctClass.toClass();
        assertEquals(2, clazz.getDeclaredMethods().length);
        return clazz;
    }

    private Class<?> createClassWithSetters() throws CannotCompileException, NotFoundException {
        final ClassPool pool = ClassPool.getDefault();
        final CtClass ctClass = pool.makeClass("my.test.Klass" + UUID.randomUUID().toString().replace("-", ""));
        ctClass.addField(new CtField(pool.get("java.lang.Object"), "id", ctClass));
        ctClass.addMethod(withBridge(CtNewMethod.make("public void setId(Object id) { this.id = id; }", ctClass)));
        ctClass.addMethod(CtNewMethod.make("public void setId(String id) { this.id = id; }", ctClass));
        final Class<?> clazz = ctClass.toClass();
        assertEquals(2, clazz.getDeclaredMethods().length);
        return clazz;
    }

    private static CtMethod withBridge(CtMethod method) {
        method.setModifiers(method.getModifiers() | 0x00000040); // Modifier.BRIDGE field not public :(
        return method;
    }
}
