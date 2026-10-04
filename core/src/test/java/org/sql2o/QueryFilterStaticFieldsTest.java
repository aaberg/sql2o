package org.sql2o;

import com.google.common.primitives.Longs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class QueryFilterStaticFieldsTest {

    private static final String URL = "jdbc:h2:mem:queryfilterstaticfields;DB_CLOSE_DELAY=-1";

    private Sql2o sql2o;

    static class Entity {
        public long ver;
        public static final Comparator<Entity> VER = new Comparator<Entity>() {
            @Override
            public int compare(final Entity o1, final Entity o2) {
                return Longs.compare(o1.ver, o2.ver);
            }
        };
    }

    @BeforeEach
    void setUp() {
        sql2o = new Sql2o(URL, "sa", "");
        sql2o.createQuery("DROP TABLE IF EXISTS TEST").executeUpdate();
        sql2o.createQuery("CREATE TABLE TEST(ver int primary key)").executeUpdate();
        sql2o.createQuery("INSERT INTO TEST VALUES(1)").executeUpdate();
    }

    @Test
    public void dontTouchTheStaticFieldTest() throws Exception {
        try(final Connection connection = sql2o.open();
        final Query query = connection.createQuery("SELECT * FROM TEST WHERE ver=1")) {
            final Entity entity = query.executeAndFetchFirst(Entity.class);
            assertThat(entity.ver, equalTo(1L));
        }
    }
}