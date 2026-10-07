package org.sql2o.quirks;

import org.sql2o.converters.Converter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetTime;
import java.util.Map;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class PostgresQuirks extends NoQuirks {
    public PostgresQuirks() {
        super();
    }

    public PostgresQuirks(Map<Class, Converter> converters) {
        super(converters);
    }

    @Override
    public boolean returnGeneratedKeysByDefault() {
        return false;
    }

    /**
     * A time with a zone is the one temporal type the driver flattens: getObject hands back a java.sql.Time with the
     * offset already dropped and folded into the zone of the session, so nothing downstream can recover it. Asking the
     * result set for an OffsetTime keeps it.
     *
     * <p>The column type code cannot be used to spot it. The driver reports both {@code time} and {@code timetz} as
     * {@link Types#TIME} and never returns {@link Types#TIME_WITH_TIMEZONE}, so the name is the only thing telling
     * them apart, the same way {@code OracleQuirks} recognises oracle timestamps by the name of their class. The
     * metadata is consulted only once the value has turned out to be a time, so the other columns pay nothing for it.
     *
     * <p>A timestamp with a zone is deliberately left alone. Postgres normalises those to an instant on the way in, so
     * the original offset is not in the column to begin with, and java.sql.Timestamp carries the instant faithfully.
     */
    @Override
    public Object getRSVal(ResultSet rs, int idx) throws SQLException {
        Object value = super.getRSVal(rs, idx);

        if (value instanceof java.sql.Time && isTimeWithTimeZone(rs, idx)) {
            return rs.getObject(idx, OffsetTime.class);
        }

        return value;
    }

    private static boolean isTimeWithTimeZone(ResultSet rs, int idx) throws SQLException {
        String typeName = rs.getMetaData().getColumnTypeName(idx);
        return "timetz".equalsIgnoreCase(typeName) || "time with time zone".equalsIgnoreCase(typeName);
    }
}