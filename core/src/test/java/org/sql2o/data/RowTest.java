package org.sql2o.data;

import org.junit.jupiter.api.Test;
import org.sql2o.Sql2oException;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Row}, the typed view over one result set row.
 *
 * <p>Beyond the getters, two things are worth pinning: a name that is not in the row fails loudly, and the lookup is
 * case insensitive only when the row was built that way.
 */
public class RowTest {

    private static final long MILLIS = 1_600_000_000_000L;

    private static final Map<String, Integer> COLUMNS = new HashMap<>();

    static {
        COLUMNS.put("dec", 0);
        COLUMNS.put("bool", 1);
        COLUMNS.put("dbl", 2);
        COLUMNS.put("flt", 3);
        COLUMNS.put("lng", 4);
        COLUMNS.put("shrt", 5);
        COLUMNS.put("byt", 6);
        COLUMNS.put("date", 7);
        COLUMNS.put("str", 8);
        COLUMNS.put("name", 9);
    }

    private static Row row(boolean caseSensitive, Map<String, Integer> columns, Object... values) {
        final Row row = new Row(columns, values.length, caseSensitive, new NoQuirks());
        for (int i = 0; i < values.length; i++) {
            row.addValue(i, values[i]);
        }
        return row;
    }

    /** One value of a usable type for every typed getter. */
    private static Row typedRow(boolean caseSensitive) {
        return row(caseSensitive, COLUMNS, new BigDecimal("1.50"), Boolean.TRUE, 2.5d, 3.5f, 4L, (short) 5, (byte) 6,
                new Date(MILLIS), "text", "Alice");
    }

    /** A row with a single column, so that index and name refer to the same value. */
    private static Row singleColumnRow(Object value) {
        return row(false, Map.of("name", 0), value);
    }

    /** Two columns, so that asMap has something to show. */
    private static Row twoColumnRow(Object first, Object second) {
        return row(false, Map.of("name", 0, "age", 1), first, second);
    }

    @Test
    public void aValueIsReadByIndex() {
        assertEquals("Alice", singleColumnRow("Alice").getObject(0));
    }

    @Test
    public void aValueIsReadByName() {
        assertEquals("Alice", singleColumnRow("Alice").getObject("name"));
    }

    @Test
    public void aValueIsHandedBackAsItIsWhenNoConversionIsAskedFor() {
        final Object value = new Object();

        assertSame(value, singleColumnRow(value).getObject(0));
    }

    @Test
    public void aNameIsMatchedIgnoringCaseUnlessTheRowIsCaseSensitive() {
        assertEquals("Alice", singleColumnRow("Alice").getObject("NAME"));

        final Row caseSensitive = new Row(Map.of("name", 0), 1, true, new NoQuirks());
        caseSensitive.addValue(0, "Alice");
        assertEquals("Alice", caseSensitive.getObject("name"));
        assertThrows(Sql2oException.class, () -> caseSensitive.getObject("NAME"));
    }

    @Test
    public void anUnknownNameSaysWhichNameWasAskedFor() {
        final Sql2oException ex = assertThrows(Sql2oException.class, () -> singleColumnRow("Alice").getObject("nope"));

        assertEquals("Column with name 'nope' does not exist", ex.getMessage());
    }

    @Test
    public void everyTypedGetterAnswersByIndex() {
        final Row row = typedRow(false);

        assertEquals(0, row.getBigDecimal(0).compareTo(new BigDecimal("1.50")));
        assertEquals(Boolean.TRUE, row.getBoolean(1));
        assertEquals(2.5d, row.getDouble(2));
        assertEquals(3.5f, row.getFloat(3));
        assertEquals(Long.valueOf(4L), row.getLong(4));
        assertEquals(Short.valueOf((short) 5), row.getShort(5));
        assertEquals(Byte.valueOf((byte) 6), row.getByte(6));
        assertEquals(new Date(MILLIS), row.getDate(7));
        assertEquals("text", row.getString(8));
    }

    @Test
    public void everyTypedGetterAnswersByName() {
        final Row row = typedRow(false);

        assertEquals(0, row.getBigDecimal("dec").compareTo(new BigDecimal("1.50")));
        assertEquals(Boolean.TRUE, row.getBoolean("bool"));
        assertEquals(2.5d, row.getDouble("dbl"));
        assertEquals(3.5f, row.getFloat("flt"));
        assertEquals(Long.valueOf(4L), row.getLong("lng"));
        assertEquals(Short.valueOf((short) 5), row.getShort("shrt"));
        assertEquals(Byte.valueOf((byte) 6), row.getByte("byt"));
        assertEquals(new Date(MILLIS), row.getDate("date"));
        assertEquals("text", row.getString("str"));
    }

    @Test
    public void aValueIsConvertedOnTheWayOut() {
        final Row row = twoColumnRow(7L, "text");

        assertEquals(Integer.valueOf(7), row.getInteger(0));
        assertEquals("text", row.getString(1));

        assertEquals(Long.valueOf(7L), row.getLong("name"));
        assertEquals("text", row.getString("age"));
    }

    /**
     * A converter that refuses the value throws ConverterException, and Row turns that into the Sql2oException every
     * other failure of this shape produces.
     */
    @Test
    public void aConverterThatRefusesTheValueIsReportedAsAConversionProblem() {
        final Row row = singleColumnRow(new Object());

        final Sql2oException byIndex = assertThrows(Sql2oException.class, () -> row.getBoolean(0));
        final Sql2oException byName = assertThrows(Sql2oException.class, () -> row.getBoolean("name"));

        assertEquals("Error converting value", byIndex.getMessage());
        assertEquals("Error converting value", byName.getMessage());
        assertTrue(byIndex.getCause() instanceof ConverterException);
    }

    /**
     * Text that is not a number takes a different route: the numeric converters parse it with the JDK parser, which
     * throws NumberFormatException, and that is unchecked, so Row never sees it as a ConverterException. The caller
     * therefore gets a NumberFormatException rather than the Sql2oException above. See docs/converter-exceptions.md.
     */
    @Test
    public void textThatIsNotANumberEscapesAsANumberFormatException() {
        final Row row = singleColumnRow("not a number");

        assertThrows(NumberFormatException.class, () -> row.getInteger(0));
        assertThrows(NumberFormatException.class, () -> row.getInteger("name"));
    }

    /**
     * NoQuirks always hands out a converter, so the "no converter for this type" guard inside Row only ever fires for
     * a driver that declines to answer. It still reaches the caller as the same Sql2oException.
     */
    @Test
    public void aQuirksWithoutAConverterIsReportedAsAConversionProblem() {
        final Row row = new Row(COLUMNS, 1, false, new NoQuirks() {
            @Override
            public <E> org.sql2o.converters.Converter<E> converterOf(Class<E> ofClass) {
                return null;
            }
        });
        row.addValue(0, "text");

        final Sql2oException ex = assertThrows(Sql2oException.class, () -> row.getObject("name", Thread.class));

        assertEquals("Error converting value", ex.getMessage());
        assertTrue(ex.getCause() instanceof ConverterException);
    }

    @Test
    public void asMapCoversEveryColumn() {
        final Map<String, Object> map = twoColumnRow("Alice", 30).asMap();

        assertEquals(2, map.size());
        assertEquals("Alice", map.get("name"));
        assertEquals(30, map.get("age"));
    }
}