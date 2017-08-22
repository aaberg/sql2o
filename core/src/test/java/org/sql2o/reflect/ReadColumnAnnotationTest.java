package org.sql2o.reflect;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.junit.Test;
import org.sql2o.reflection.PojoMetadata;

import javax.persistence.Column;
import java.util.Collections;

@SuppressWarnings("unused")
public class ReadColumnAnnotationTest extends TestCase {

    @Test
    public void testNoAnnotationPojo() {
        PojoMetadata metadata = newPojoMetadata(NoAnnotation.class);
        assertNotNull(metadata.getPropertySetterIfExists("field1"));
    }

    @Test
    public void testNoAnnotationSetterPojo() {
        PojoMetadata metadata = newPojoMetadata(NoAnnotationSetter.class);
        assertNotNull(metadata.getPropertySetterIfExists("field1"));
    }

    @Test
    public void testOnlyOneAnnotationFieldPojo() {
        PojoMetadata metadata = newPojoMetadata(OnlyOneAnnotationField.class);
        assertNotNull(metadata.getPropertySetterIfExists("field_1"));
    }

    @Test
    public void testOneAnnotationFieldPojo() {
        PojoMetadata metadata = newPojoMetadata(OneAnnotationField.class);
        assertNotNull(metadata.getPropertySetterIfExists("field_1"));
        assertNotNull(metadata.getPropertySetterIfExists("field2"));
    }

    @Test
    public void testAnnotationFieldAndSetterPojo() {
        PojoMetadata metadata = newPojoMetadata(AnnotationFieldAndASetter.class);
        assertNotNull(metadata.getPropertySetterIfExists("field_1"));
        assertNotNull(metadata.getPropertySetterIfExists("field2"));
        assertNotNull(metadata.getPropertySetterIfExists("field_3"));
        assertNotNull(metadata.getPropertySetterIfExists("field4"));
    }

    @Test
    public void testUppercaseAnnotationFieldPojo() {
        PojoMetadata metadata = newPojoMetadata(UpperCaseAnnotationField.class);
        assertNotNull(metadata.getPropertySetterIfExists("field_1"));
    }

    private PojoMetadata newPojoMetadata(Class<?> clazz) {
        return new PojoMetadata(clazz, false, false, ImmutableMap.<String, String> of(), true, Collections.<String>emptySet());
    }

    private static class NoAnnotation {
        private String field1;
    }

    private static class NoAnnotationSetter {
        private String field1;

        void setField1(String field1) {
            this.field1 = field1;
        }
    }

    private static class OnlyOneAnnotationField {
        @Column(name = "field_1")
        private String field1;
    }

    private static class OneAnnotationField {
        @Column(name = "field_1")
        private String field1;
        private String field2;
    }

    private static class AnnotationFieldAndASetter {
        @Column(name = "field_1")
        private String field1;
        private String field2;
        private String field3;
        private String field4;

        @Column(name = "field_3")
        void setField3(String field3) {
            this.field3 = field3;
        }

        void setField4(String field4) {
            this.field4 = field4;
        }
    }
    
    private static class UpperCaseAnnotationField {
        @Column(name = "FIELD_1")
        private String field1;
    }

}
