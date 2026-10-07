package org.sql2o.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TableTest {

    private static final Column COLUMN = new Column("name", 0, "VARCHAR");

    @Test
    public void aTableHandsBackWhatItWasBuiltWith() {
        final Table table = new Table("MY_TABLE", List.of(), List.of(COLUMN));

        assertEquals("MY_TABLE", table.getName());
        assertEquals(List.of(), table.rows());
        assertEquals(List.of(COLUMN), table.columns());
    }

    @Test
    public void asListViewsEachRowAsAMap() {
        final Table table = new Table("MY_TABLE", List.of(rowWith("Alice"), rowWith("Bob")), List.of(COLUMN));

        final List<Map<String, Object>> asList = table.asList();

        assertEquals(2, asList.size());
        assertEquals("Alice", asList.get(0).get("name"));
        assertEquals("Bob", asList.get(1).get("name"));
    }

    @Test
    public void asListIsAViewOverTheRowsItWasBuiltFrom() {
        final Row row = rowWith("Alice");
        final Table table = new Table("MY_TABLE", List.of(row), List.of(COLUMN));

        assertSame(row, table.rows().get(0));
    }

    private static Row rowWith(String value) {
        final Map<String, Integer> columns = Map.of("name", 0);
        final Row row = new Row(columns, 1, false, new org.sql2o.quirks.NoQuirks());
        row.addValue(0, value);
        return row;
    }
}