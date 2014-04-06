package org.sql2o;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class SqlParameter {

    private int sqlType;
    private Object value;

    public SqlParameter(int sqlType, Object value) {
        this.sqlType = sqlType;
        this.value = value;
    }

    public int getSqlType() {
        return sqlType;
    }

    public Object getValue() {
        return value;
    }
}
