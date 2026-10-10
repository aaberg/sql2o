package org.sql2o.params;

import org.sql2o.Sql2o;

import java.util.List;

/**
 * The typed parameters against Derby, through the quirks this extension ships.
 *
 * <p>It is the strictest of the databases here in one respect and the most forgiving in another. It has no uuid type and
 * no column type carrying a time zone, so a uuid is stored as sixteen bytes and an offset is dropped on the way in. What
 * it does have that the others do not is a real {@code boolean} column, and a driver that answers a {@code smallint}
 * with an {@link Integer} rather than a {@link Short}, which the matrix has to say something about.
 *
 * <p>Nothing is left out of this matrix. Every case the base class knows is run here, unlike with hsqldb, whose driver
 * shifts a {@code java.time.LocalTime} by the offset of the jvm on the way in.
 */
public class DerbyTypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:derby:memory:typedParametersDerby;create=true", null, null);

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
            // A real boolean column, unlike hsqldb, which stores one as a smallint.
            case BOOLEAN -> "boolean";
            case DECIMAL -> "decimal(12,4)";
            case DATE -> "date";
            case TIME -> "time";
            case TIMESTAMP -> "timestamp";
            // No uuid type, so a uuid is the sixteen bytes of a for bit data column, which is what the quirks write and
            // what the converter reads back. char(16) for bit data would hold the same bytes but pads what it holds to
            // the length of the column, and varchar for bit data does not, so a uuid of sixteen bytes is what comes back.
            case UUID_KIND -> "varchar(16) for bit data";
            case BLOB -> "blob";
            case CLOB -> "clob";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSDERBY";
    }

    @Override
    protected List<ParameterCase> cases() {
        // Nothing is filtered out. The base cases are the whole story here, and a subclass that overrides this to say so
        // explicitly is worth more than one that inherits it by silence.
        return super.cases();
    }
}