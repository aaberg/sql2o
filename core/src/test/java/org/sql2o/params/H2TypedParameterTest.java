package org.sql2o.params;

import org.sql2o.Sql2o;

/**
 * The typed parameters against H2, one of the two databases core itself runs on. The column kinds are the obvious
 * ones; what H2 has no answer for is written down in {@link AbstractTypedParameterTest#cases()}.
 */
public class H2TypedParameterTest extends AbstractTypedParameterTest {

    private static final String URL = "jdbc:h2:mem:typedParametersH2;DB_CLOSE_DELAY=-1";

    private final Sql2o sql2o = new Sql2o(URL, "sa", "");

    @Override
    protected Sql2o sql2o() {
        return sql2o;
    }

    @Override
    protected String columnType(String columnKind) {
        return switch (columnKind) {
            case STRING, UUID_KIND -> "varchar(36)";
            case SMALL, INTEGER, BIGINT, BOOLEAN -> switch (columnKind) {
                case SMALL -> "smallint";
                case BIGINT -> "bigint";
                case BOOLEAN -> "boolean";
                default -> "integer";
            };
            case REAL -> "real";
            case DOUBLE -> "double";
            case DECIMAL -> "decimal(12,4)";
            case DATE -> "date";
            case TIME -> "time";
            case TIMESTAMP -> "timestamp";
            case BLOB -> "binary large object";
            case CLOB -> "clob";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSH2";
    }
}
