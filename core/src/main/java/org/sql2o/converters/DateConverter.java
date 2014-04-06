package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

/**
 * Used by sql2o to convert a value from the database into a {@link Date}.
 */
public class DateConverter implements Converter<Date> {
    
    public Date convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }
        
        if (Date.class.isAssignableFrom( val.getClass() )){
            return (Date)val;
        }
        
        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Date");
    }

    public void addParameter(StatementParameterSetter stmt, String name, Date val) throws SQLException {
        if (val == null) {
            stmt.setNull(name, Types.DATE);
        }
        else {
            // by default add a timestamp, because it works with DATE, DATETIME, TIMESTAMP columns
            Timestamp timestamp = new Timestamp(val.getTime());
            stmt.setTimestamp(name, timestamp);
        }
    }

    public Object toDatabaseParam(Date val) {
        return new Timestamp(val.getTime());
    }
}
