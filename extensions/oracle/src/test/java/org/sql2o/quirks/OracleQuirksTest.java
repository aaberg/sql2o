package org.sql2o.quirks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.OracleUUIDConverter;
import org.sql2o.converters.UUIDConverter;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link OracleQuirks}. The getRSVal tests read from a real oracle, because the interesting branch only opens
 * up for an oracle.sql.TIMESTAMP, which no in-memory database hands out. Needs {@code docker compose up -d}. Binding a
 * uuid is not repeated here: the issues test already writes one into a raw(16) column and reads it back, which says
 * more about the sixteen bytes than a stub statement could.
 */
public class OracleQuirksTest {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";

    private final OracleQuirks quirks = new OracleQuirks();

    /**
     * The driver is registered by hand because the url carries the legacy {@code @host:port:SID} form, which the
     * service loader cannot resolve on its own.
     */
    @BeforeAll
    public static void registerTheDriver() {
        try {
            Class<?> oracleDriverClass =
                    OracleQuirksTest.class.getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver) oracleDriverClass.getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Test
    public void itRegistersTheOracleUuidConverterByDefault() {
        assertThat(quirks.converterOf(UUID.class), instanceOf(OracleUUIDConverter.class));
    }

    /**
     * The map replaces the default set instead of adding to it, so a caller who brings their own converters loses the
     * oracle uuid converter unless they add it themselves. What is left is whatever the global registry holds.
     */
    @Test
    public void itTakesTheConvertersToUseFromTheCaller() {
        Map<Class, Converter> converters = new HashMap<>();

        assertThat(new OracleQuirks(converters).converterOf(UUID.class), instanceOf(UUIDConverter.class));

        Converter<String> stringConverter = new Converter<String>() {
            @Override
            public String convert(Object val) {
                return "converted";
            }

            @Override
            public Object toDatabaseParam(String val) {
                return val;
            }
        };
        converters.put(String.class, stringConverter);

        assertEquals(stringConverter, new OracleQuirks(converters).converterOf(String.class));
    }

    /** Oracle has no returning clause, so sql2o must not ask the driver for generated keys unless told to. */
    @Test
    public void itDoesNotWantGeneratedKeys() {
        assertFalse(quirks.returnGeneratedKeysByDefault());
        assertTrue(new NoQuirks().returnGeneratedKeysByDefault());
    }

    /**
     * A TIMESTAMP column comes back from getObject as an oracle.sql.TIMESTAMP, which no jdk class can be built from, so
     * the quirks turns it into the jdk flavour instead.
     */
    @Test
    public void itReadsAnOracleTimestampAsAJdkTimestamp() throws SQLException {
        Object value = readFirstColumn("select timestamp '2020-01-01 12:34:56' val from dual");

        assertThat(value, instanceOf(Timestamp.class));
        assertEquals(Timestamp.valueOf("2020-01-01 12:34:56"), value);
    }

    @Test
    public void itLeavesANullColumnAlone() throws SQLException {
        assertNull(readFirstColumn("select cast(null as timestamp) val from dual"));
    }

    @Test
    public void itLeavesEverythingElseAlone() throws SQLException {
        assertEquals("test", readFirstColumn("select 'test' val from dual"));
        assertEquals("42", readFirstColumn("select to_char(42) val from dual"));

        assertThat(readFirstColumn("select cast(null as varchar2(10)) val from dual"), nullValue());
    }

    /** Runs the query, hands the single row to the quirks the way a mapper would, and closes everything again. */
    private Object readFirstColumn(String sql) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(URL, "system", "testpassword");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return quirks.getRSVal(resultSet, 1);
        }
    }
}
