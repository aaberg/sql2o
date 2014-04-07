package org.sql2o.converters;

import java.util.Date;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class DateConverterNoTimestamp extends DateConverter {
    @Override
    public Object toDatabaseParam(Date val) {
        return new java.sql.Date(val.getTime());
    }
}
