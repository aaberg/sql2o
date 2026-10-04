package org.sql2o;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author zapodot
 */
public class QueryArrayTest {

    private static final String URL = "jdbc:h2:mem:queryarraytest;MODE=Oracle;DB_CLOSE_DELAY=-1";

    private Sql2o sql2o;

    private static class Foo {
        public int bar;
    }

    @BeforeEach
    void setUp() {
        sql2o = new Sql2o(URL, "sa", "");
        sql2o.createQuery("DROP TABLE IF EXISTS FOO").executeUpdate();
        sql2o.createQuery("CREATE TABLE FOO(BAR int PRIMARY KEY)").executeUpdate();
        sql2o.createQuery("INSERT INTO FOO VALUES(1)").executeUpdate();
        sql2o.createQuery("INSERT INTO FOO VALUES(2)").executeUpdate();
    }

    @Test
    public void arrayTest() throws Exception {
        try(final Connection connection = sql2o.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {
            final List<Foo> foos = query.addParameter("bars", 1, 2).executeAndFetch(Foo.class);
            assertThat(foos.size(), equalTo(2));

        }
    }

    @Test
    public void emptyArrayTest() throws Exception {
        try(final Connection connection = sql2o.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

            final List<Foo> noFoos = query.addParameter("bars", new Integer[]{}).executeAndFetch(Foo.class);
            assertThat(noFoos.size(), equalTo(0));
        }
    }
}