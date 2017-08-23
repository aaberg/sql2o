package org.sql2o;

import com.google.common.primitives.Longs;
import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.Comparator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class QueryFilterStaticFieldsTest {

    @Rule
    public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
                                                                   .withInitialSql(
                                                                           "CREATE TABLE TEST(ver int primary key); INSERT INTO TEST VALUES(1);")
                                                                   .build();

    static class Entity {
        public long ver;
        public static final Comparator<Entity> VER = new Comparator<Entity>() {
            @Override
            public int compare(final Entity o1, final Entity o2) {
                return Longs.compare(o1.ver, o2.ver);
            }
        };
    }

    @Test
    public void dontTouchTheStaticFieldTest() throws Exception {
        final Sql2o dataBase = new Sql2o(databaseRule.getDataSource());
        try(final Connection connection = dataBase.open(); 
        final Query query = connection.createQuery("SELECT * FROM TEST WHERE ver=1")) {
            final Entity entity = query.executeAndFetchFirst(Entity.class);
            assertThat(entity.ver, equalTo(1L));
        }
    }
}