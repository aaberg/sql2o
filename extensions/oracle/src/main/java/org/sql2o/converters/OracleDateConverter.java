package org.sql2o.converters;

import oracle.sql.Datum;
import oracle.sql.TIMESTAMP;
import oracle.sql.TIMESTAMPTZ;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Created by lars on 01.05.14.
 */
public class OracleDateConverter extends DateConverter implements ConvertersProvider {
    @Override
    public Date convert(Object val) throws ConverterException {

        // TIMESTAMPTZ is a sibling of TIMESTAMP rather than a subclass, and the two want different accessors. Its
        // timestampValue() refuses to run on an instance that came out of a result set, which is where every mapped
        // column comes from, while offsetDateTimeValue() is happy to read the stored offset on its own. Taking the
        // instant from there keeps the value honest, rather than shifting it into the zone of the jvm.
        if (val instanceof TIMESTAMPTZ) {
            try {
                return Timestamp.from(((TIMESTAMPTZ) val).offsetDateTimeValue().toInstant());
            } catch (SQLException e) {
                throw cannotRead(val, e);
            }
        }

        if (val instanceof TIMESTAMP) {
            try {
                return ((TIMESTAMP) val).timestampValue();
            } catch (SQLException e) {
                throw cannotRead(val, e);
            }
        }

        // TIMESTAMPLTZ and anything else oracle sends as a datum. These keep their zone as an index into the database
        // time zone table, so reading one needs a connection that a converter is never handed, and this will fail for
        // them. The mapping path avoids it by letting OracleQuirks.getRSVal read the column instead.
        if (val instanceof Datum) {
            try {
                return ((Datum) val).timestampValue();
            } catch (SQLException e) {
                throw cannotRead(val, e);
            }
        }

        return super.convert(val);
    }

    private static ConverterException cannotRead(Object val, SQLException cause) {
        return new ConverterException("Error trying to convert " + val.getClass().getName() + " to java.util.Date",
                cause);
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(Date.class,  new OracleDateConverter());
    }
}
