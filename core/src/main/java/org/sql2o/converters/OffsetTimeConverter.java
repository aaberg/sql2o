package org.sql2o.converters;

import java.sql.Time;
import java.time.OffsetTime;

public class OffsetTimeConverter extends ConverterBase<OffsetTime> {

    @Override
    public OffsetTime convert(Object val) throws ConverterException {
        if (val instanceof OffsetTime) {
            return (OffsetTime) val;
        }

        if (val instanceof Time) {
            return ((Time) val).toLocalTime().atOffset(OffsetTime.now().getOffset());
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " to java.time.OffsetTime");
    }
}
