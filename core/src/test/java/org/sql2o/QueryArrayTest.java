package org.sql2o;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;
import org.zapodot.junit.db.common.CompatibilityMode;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author zapodot
 */
public class QueryArrayTest {

    private static class Foo {
        public int bar;
    }
    
    @Rule
    public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
            .withMode(CompatibilityMode.Oracle)
            .withInitialSql("CREATE TABLE FOO(BAR int PRIMARY KEY); INSERT INTO FOO VALUES(1); INSERT INTO FOO VALUES(2)")
            .build();

    @Test
    public void arrayTest() throws Exception {
        final Sql2o database = new Sql2o(databaseRule.getDataSource());
        try(final Connection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {
            final List<Foo> foos = query.addParameter("bars", 1, 2).executeAndFetch(Foo.class);
            assertThat(foos.size(), equalTo(2));

        }
    }

    @Test
    public void emptyArrayTest() throws Exception {
        final Sql2o database = new Sql2o(databaseRule.getDataSource());

        try(final Connection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

            final List<Foo> noFoos = query.addParameter("bars", new Integer[]{}).executeAndFetch(Foo.class);
            assertThat(noFoos.size(), equalTo(0));
        }
    }
}