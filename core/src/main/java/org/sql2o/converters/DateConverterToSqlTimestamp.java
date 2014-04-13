package org.sql2o.converters;

import java.sql.Timestamp;
import java.util.Date;

/**
 * User: dimzon
 * Date: 4/13/14
 * Time: 3:19 AM
 */
public class DateConverterToSqlTimestamp extends DateConverter {
    @Override
    public Object toDatabaseParam(Date val) {
        return val instanceof Timestamp ? val
                : new Timestamp(val.getTime());
    }
}
