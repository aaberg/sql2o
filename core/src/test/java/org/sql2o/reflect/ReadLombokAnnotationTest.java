package org.sql2o.reflect;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import lombok.Data;
import org.junit.Test;
import org.sql2o.reflection.PojoMetadata;

@SuppressWarnings("unused")
public class ReadLombokAnnotationTest
    extends TestCase {

    @Test
    public void testBooleanGetter() {
        PojoMetadata metadata = newPojoMetadata(BooleanAnnotation.class);
        assertNotNull(metadata.getPropertyGetterIfExists("field1"));
        assertNotNull(metadata.getPropertyGetterIfExists("field2"));
    }

    private PojoMetadata newPojoMetadata(Class<?> clazz) {
        return new PojoMetadata(clazz, false, false, ImmutableMap.<String, String> of(), true);
    }

    @Data
    private static class BooleanAnnotation {
        private boolean field1;
        private Boolean field2;
    }


}
