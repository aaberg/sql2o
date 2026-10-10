package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The mapping path used to read columns with {@code ResultSet.getObject} directly, which left {@link Quirks#getRSVal}
 * as dead code for pojos and records: a driver with an opinion about how it wants its own types read had no way to say
 * so. Oracle needed exactly that, because the objects it hands out for the time zone aware timestamp columns cannot
 * convert themselves, and only the result set can convert them. These tests pin the path down for both object kinds.
 */
public class MapperUsesTheQuirksTest {

    private static final String URL = "jdbc:h2:mem:mapperUsesTheQuirks;DB_CLOSE_DELAY=-1";

    /**
     * Stands in for a driver that hands out values the mapper cannot use on its own. Shouting in getRSVal is enough to
     * tell whether a value reached the field by that route or straight from the result set.
     */
    private static class ShoutingQuirks extends NoQuirks {
        @Override
        public Object getRSVal(ResultSet rs, int idx) throws SQLException {
            Object value = super.getRSVal(rs, idx);
            return value instanceof String ? ((String) value).toUpperCase() : value;
        }
    }

    private final Sql2o sql2o = new Sql2o(URL, "sa", "", new ShoutingQuirks());

    public static class Pojo {
        public String text;
    }

    public record Row(String text) {}

    @Test
    public void aPojoSeesTheValueTheQuirksReturned() {
        try (Connection connection = sql2o.open()) {
            Pojo pojo = connection.createQuery("select 'hello' as text").executeAndFetchFirst(Pojo.class);

            assertEquals("HELLO", pojo.text);
        }
    }

    @Test
    public void aRecordSeesTheValueTheQuirksReturned() {
        try (Connection connection = sql2o.open()) {
            Row row = connection.createQuery("select 'hello' as text").executeAndFetchFirst(Row.class);

            assertEquals("HELLO", row.text());
        }
    }

    @Test
    public void aNullColumnStaysNull() {
        try (Connection connection = sql2o.open()) {
            Pojo pojo = connection.createQuery("select cast(null as varchar(20)) as text")
                    .executeAndFetchFirst(Pojo.class);

            assertNull(pojo.text);
        }
    }

    /** Columns the quirks leaves alone must come through untouched, quirks or not. */
    @Test
    public void valuesTheQuirksDoesNotTouchArriveUnchanged() {
        try (Connection connection = sql2o.open()) {
            Timestamp timestamp = connection.createQuery("select cast('2020-01-01 12:34:56' as timestamp) as text")
                    .executeScalar(Timestamp.class);

            assertEquals(Timestamp.valueOf("2020-01-01 12:34:56"), timestamp);

            List<Integer> numbers = connection.createQuery("select 42 as text").executeScalarList(Integer.class);

            assertEquals(List.of(42), numbers);
        }
    }
}
