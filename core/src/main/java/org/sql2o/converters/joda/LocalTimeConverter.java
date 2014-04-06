package org.sql2o.converters.joda;

import org.joda.time.LocalTime;
import org.sql2o.SqlParameter;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

/**
 * Created by lars on 12/18/13.
 */
public class LocalTimeConverter implements Converter<LocalTime> {

    public LocalTime convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }

        if (!(val instanceof Time)){
            throw new ConverterException("Don't know how to convert from type '" + val.getClass().getName() + "' to type '" + LocalTime.class.getName() + "'");
        }

        return new LocalTime(val);
    }

    public Object toDatabaseParam(LocalTime val) {
        return new Time(val.toDateTimeToday().getMillis());
    }

    public void addParameter(StatementParameterSetter stmt, String name, LocalTime val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.TIME);
        }
        else {
            Time sqlTime = new Time(val.toDateTimeToday().getMillis());
            stmt.setTime(name, sqlTime);
        }
    }
}
