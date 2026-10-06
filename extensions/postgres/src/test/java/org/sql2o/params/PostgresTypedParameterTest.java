package org.sql2o.params;

import org.sql2o.Sql2o;

/**
 * The typed parameters against PostgreSQL.
 *
 * <p>The quirks come from the service loader rather than being named here, so that this also covers the wiring that
 * ships in the jar. What PostgreSQL can and cannot hold is measured rather than assumed; see {@link #cases()}.
 */
public class PostgresTypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:postgresql://localhost:15432/postgres", "testuser", "testpassword");

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
            case DOUBLE -> "double precision";
            case BOOLEAN -> "boolean";
            case DECIMAL -> "numeric(12,4)";
            case DATE -> "date";
            case TIME -> "time";
            case TIMESTAMP -> "timestamp";
            case UUID_KIND -> "uuid";
            case BLOB -> "bytea";
            // Postgres has no clob; text is the same thing under another name.
            case CLOB -> "text";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSPG";
    }
}
