package org.sql2o.params;

import org.sql2o.Sql2o;

/**
 * The typed parameters against PostgreSQL.
 *
 * <p>The quirks come from the service loader rather than being named here, so that this also covers the wiring that
 * ships in the jar.
 *
 * <p>Postgres takes almost all of the matrix as it is, including an {@link java.time.OffsetDateTime}, which it has a
 * {@code timestamptz} to keep. An {@link java.time.Instant} it cannot place at all — Can&apos;t infer the SQL type to use
 * for an instance of java.time.Instant — so the quirks register {@code InstantToTimestampConverter} from core for that one
 * and nothing else is touched.
 *
 * <p>This database has no README of its own, so anything measured here that a reader would want written down is written
 * down here instead.
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
