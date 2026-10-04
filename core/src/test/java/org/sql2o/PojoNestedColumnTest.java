package org.sql2o;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the dotted column name path in {@link org.sql2o.reflection2.PojoBuilder#withValue(String, Object)},
 * where a column named {@code sub.val} is mapped into a nested property.
 */
public class PojoNestedColumnTest {

    private static final String URL = "jdbc:h2:mem:ponestedcolumn;DB_CLOSE_DELAY=-1";

    private Sql2o sql2o;

    public static class SubPojo {
        public int val;
    }

    public static class OuterPojo {
        public SubPojo sub;
    }

    @BeforeEach
    void setUp() {
        sql2o = new Sql2o(URL, "sa", "");
    }

    @Test
    public void dottedColumnName_populatesNestedProperty() {
        try (Connection con = sql2o.open()) {
            OuterPojo pojo = con.createQuery("select 42 as \"sub.val\" from (values(0))")
                    .executeAndFetchFirst(OuterPojo.class);

            assertNotNull(pojo.sub);
            assertEquals(42, pojo.sub.val);
        }
    }

    @Test
    public void dottedColumnName_withoutMatchingProperty_throwsNullPointerException() {
        // Characterisation test: the dotted branch of withValue dereferences the looked up sub property
        // without the null check that the non-dotted branch has, so an unmapped prefix ends in an NPE
        // instead of "Could not map ... to any property.". Invert this expectation once that is fixed.
        try (Connection con = sql2o.open()) {
            assertThrows(NullPointerException.class, () ->
                    con.createQuery("select 42 as \"nothing.here\" from (values(0))")
                            .executeAndFetchFirst(OuterPojo.class));
        }
    }
}