package org.sql2o.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColumnTest {

    @Test
    public void aColumnRemembersWhatItWasBuiltWith() {
        final Column column = new Column("MY_COLUMN", 2, "VARCHAR");

        assertEquals("MY_COLUMN", column.getName());
        assertEquals(2, column.getIndex());
        assertEquals("VARCHAR", column.getType());
    }

    @Test
    public void aColumnDescribesItselfWithNameAndType() {
        assertEquals("MY_COLUMN (VARCHAR)", new Column("MY_COLUMN", 0, "VARCHAR").toString());
    }
}