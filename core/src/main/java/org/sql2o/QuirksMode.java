package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.quirks.Db2Quirks;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.PostgresQuirks;
import org.sql2o.quirks.Quirks;
import org.sql2o.tools.OutParameterGetter;

import java.io.InputStream;
import java.sql.*;

/**
 * Use {@link org.sql2o.quirks.Quirks}.
 */
@Deprecated
public enum QuirksMode implements Quirks {

    None(new NoQuirks()),
    DB2(new Db2Quirks()),
    PostgreSQL(new PostgresQuirks()),
    MSSqlServer(new NoQuirks());

    private final Quirks quirks;

    public <E> Converter<E> converterOf(Class<E> ofClass) {
        return quirks.converterOf(ofClass);
    }

    public <E> OutParameterGetter<E> outParamGetterOf(Class<E> ofClass) {
        return quirks.outParamGetterOf(ofClass);
    }


    public String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
        return quirks.getColumnName(meta, colIdx);
    }

    public boolean returnGeneratedKeysByDefault() {
        return quirks.returnGeneratedKeysByDefault();
    }

    public void setParameter(PreparedStatement statement, int paramIdx, Object value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, InputStream value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, int value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramidx, Integer value) throws SQLException {
        quirks.setParameter(statement, paramidx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, long value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, Long value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, String value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, Timestamp value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void setParameter(PreparedStatement statement, int paramIdx, Time value) throws SQLException {
        quirks.setParameter(statement, paramIdx, value);
    }

    public void registerOutParameter(ProcedureCall procedureCall, OutParameter parameter) throws SQLException {
        quirks.registerOutParameter(procedureCall, parameter);
    }

    public Object getRSVal(ResultSet rs, int idx) throws SQLException {
        return this.quirks.getRSVal(rs, idx);
    }

    private QuirksMode(Quirks quirks) {
        this.quirks = quirks;
    }




}
