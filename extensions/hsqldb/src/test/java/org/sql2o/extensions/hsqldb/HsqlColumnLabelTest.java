package org.sql2o.extensions.hsqldb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mapping by the label an alias was given, which is where hsqldb differs from most databases and was the subject of
 * issue #9, https://github.com/aaberg/sql2o/issues/9.
 *
 * <p>{@code ResultSet.getColumnName()} answers with the name of the column underneath, even when the query gave an
 * alias, while {@code getColumnLabel()} answers with the alias. sql2o reads the label, so {@code select id, val theVal}
 * maps onto a property called {@code theVal}; reading the name instead would leave the value with nowhere to go.
 *
 * <p>This used to be one case among many in {@code IssuesTest} of core, which ran on this database as well as on h2. It
 * is here because it is a statement about hsqldb, and in core it would be a test that passes without meaning anything.
 */
public class HsqlColumnLabelTest {

    private static final String URL = "jdbc:hsqldb:mem:columnlabel";

    private static final String TABLE = "HSQLCOLUMNLABEL";

    private static Sql2o sql2o;

    @BeforeAll
    public static void openSql2o() {
        sql2o = new Sql2o(URL, "SA", "");

        dropTheTable();

        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table " + TABLE
                    + " (id integer identity primary key, val varchar(50))").executeUpdate();

            String insertSql = "insert into " + TABLE + "(val) values (:val)";
            for (String value : List.of("something", "something else", "something third")) {
                connection.createQuery(insertSql).addParameter("val", value).executeUpdate();
            }
        }
    }

    @AfterAll
    public static void dropTheTables() {
        dropTheTable();
    }

    private static void dropTheTable() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table " + TABLE).executeUpdate();
        } catch (RuntimeException ignored) {
            // The table is not there, which is exactly the state this was called in.
        }
    }

    @Test
    public void aQueryWithAnAliasMapsOntoThePropertyTheAliasNames() {
        try (Connection connection = sql2o.open()) {
            List<Issue9Pojo> pojos = connection
                    .createQuery("select id, val theVal from " + TABLE)
                    .executeAndFetch(Issue9Pojo.class);

            assertEquals(3, pojos.size());
            assertEquals("something", pojos.get(0).theVal);
        }
    }

    public static class Issue9Pojo {
        public int id;
        public String theVal;
    }
}