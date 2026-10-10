package org.sql2o.params;

import org.sql2o.Sql2o;

import java.util.List;

/**
 * The typed parameters against MariaDB, through the quirks this extension ships.
 */
public class MysqlTypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:mysql://localhost:13306/testdb", "testuser", "testpassword");

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
            case REAL -> "float";
            case DOUBLE -> "double";
            case BOOLEAN -> "tinyint(1)";
            case DECIMAL -> "decimal(12,4)";
            case DATE -> "date";
            case TIME -> "time";
            // With fractional seconds: a plain timestamp keeps whole seconds only, and the Timestamp case carries
            // milliseconds the base test expects back.
            case TIMESTAMP -> "timestamp(3)";
            // No uuid type, so a uuid is the sixteen bytes of a binary column, which is what the quirks write and
            // what the converter reads back.
            case UUID_KIND -> "binary(16)";
            case BLOB -> "blob";
            case CLOB -> "text";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSMYSQL";
    }

    /**
     * {@code blob} is a reserved word in MySQL and MariaDB, so the wide columns go quoted. The labels come back
     * without the quotes, which is why the fields they land in keep their names.
     */
    @Override
    protected String blobColumn() {
        return "`blob`";
    }

    @Override
    protected String clobColumn() {
        return "`clob`";
    }

    @Override
    protected List<ParameterCase> cases() {
        return super.cases();
    }
}
