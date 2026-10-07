package org.sql2o.params;

import org.sql2o.Sql2o;

import java.util.List;

/**
 * The typed parameters against Oracle.
 *
 * <p>The quirks come from the service loader rather than being named here, so that this also covers the wiring that
 * ships in the jar. Oracle has no uuid type and no time type, and a date is a point in time with a time part; which of
 * those bite is measured here rather than assumed. See {@link #cases()}.
 */
public class OracleTypedParameterTest extends AbstractTypedParameterTest {

    private final Sql2o sql2o = new Sql2o("jdbc:oracle:thin:@localhost:1521:XE", "system", "testpassword");

    @Override
    protected Sql2o sql2o() {
        return sql2o;
    }

    @Override
    protected String columnType(String columnKind) {
        return switch (columnKind) {
            case STRING -> "varchar2(36)";
            case SMALL -> "number(5)";
            case INTEGER -> "number(10)";
            case BIGINT -> "number(19)";
            case REAL -> "float(24)";
            case DOUBLE -> "double precision";
            case BOOLEAN -> "number(1)";
            case DECIMAL -> "number";
            case DATE -> "date";
            case TIMESTAMP -> "timestamp";
            // Oracle has no uuid type. A uuid is kept as the sixteen bytes of raw(16), which is what the quirks
            // write and what the converter reads back.
            case UUID_KIND -> "raw(16)";
            case BLOB -> "blob";
            case CLOB -> "clob";
            default -> throw new IllegalArgumentException("no column type for " + columnKind);
        };
    }

    @Override
    protected String tableName() {
        return "TYPEDPARAMSORA";
    }

    @Override
    protected List<ParameterCase> cases() {
        return super.cases().stream()
                // Oracle has no time type: a DATE carries a time part, so there is nowhere to put a value that is a
                // time of day and nothing else, and reading one back out of a DATE would hand it a date as well.
                .filter(testCase -> !TIME.equals(testCase.columnKind()))
                .map(this::whatOracleGivesBack)
                .toList();
    }

    /**
     * A NUMBER has no scale of its own, so 1234.56 comes back as the two digits it went in with, where the
     * decimal(12,4) columns of the other databases pad it out to four.
     */
    private ParameterCase whatOracleGivesBack(ParameterCase testCase) {
        return DECIMAL.equals(testCase.columnKind())
                ? new ParameterCase(testCase.label(), testCase.type(), testCase.value(), testCase.value(),
                        testCase.columnKind())
                : testCase;
    }
}
