package org.sql2o.params;

import org.sql2o.Sql2o;


/**
 * The typed parameters against DB2.
 *
 * <p>The quirks come from the service loader rather than being named here, so that this also covers the wiring that
 * ships in the jar. What DB2 has no answer for is measured here rather than assumed. See {@link #cases()}.
 */
public class Db2TypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:db2://localhost:50000/testdb", "db2inst1", "testpassword");

    @Override
    protected Sql2o sql2o() {
        return sql2o;
    }

    @Override
    protected String columnType(String columnKind) {
        return switch (columnKind) {
            case STRING -> "varchar(36)";
            case SMALL -> "smallint";
            case INTEGER -> "integer";
            case BIGINT -> "bigint";
            case REAL -> "real";
            case DOUBLE -> "double";
            case BOOLEAN -> "boolean";
            case DECIMAL -> "decimal(12,4)";
            case DATE -> "date";
            case TIME -> "time";
            case TIMESTAMP -> "timestamp";
            // Db2 has no uuid type either, so a uuid is kept as sixteen bytes of binary data, which is what the
            // quirks write and what the converter reads back. Of the three ways of saying that, the CHAR form is
            // equivalent here, since a uuid is always sixteen bytes and its blank padding never comes into play,
            // and BINARY only exists from 11.5 on, so the one that costs nothing to support is the one to use.
            case UUID_KIND -> "varchar(16) for bit data";
            case BLOB -> "blob";
            case CLOB -> "clob";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSDB2";
    }
}
