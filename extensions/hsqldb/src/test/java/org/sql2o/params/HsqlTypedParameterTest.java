package org.sql2o.params;

import org.sql2o.Sql2o;

import java.time.LocalTime;
import java.util.List;

/**
 * The typed parameters against HSQLDB, through the quirks this extension ships.
 *
 * <p>It is the narrowest of the databases here: it has no uuid type, so a uuid is stored as sixteen bytes, and the
 * driver shifts a {@link LocalTime} by the offset of the jvm on the way in. Both are measured here rather than assumed.
 */
public class HsqlTypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:hsqldb:mem:typedParametersHsql", "SA", "");

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
            // No uuid type either, so a uuid is the sixteen bytes of a binary column, which is what the quirks write and
            // what the converter reads back. varbinary and longvarbinary would hold them just as well; binary is the one
            // whose fixed length has nothing to pad, because a uuid is always sixteen bytes.
            case UUID_KIND -> "binary(16)";
            case BLOB -> "blob";
            case CLOB -> "clob";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSHSQL";
    }

    @Override
    protected List<ParameterCase> cases() {
        return super.cases().stream()
                // A java.time.LocalTime goes in shifted by the offset of the jvm and comes back shifted by it too, so
                // 12:34:56 arrives as 15:34:56 on a machine set to Europe/Moscow. Binding it through plain jdbc, with
                // no sql2o involved, stores the shifted value as well, so the driver does it on the way in.
                // LocalTimeConverterTest in core says the same about HSQLDB converting a local time before storing it,
                // and skips its own assertions here for the same reason.
                .filter(testCase -> testCase.type() != LocalTime.class)
                .toList();
    }
}