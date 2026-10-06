package org.sql2o.converters;

import oracle.sql.Datum;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * Created by lars on 01.05.14.
 */
public class OracleDateConverter extends DateConverter implements ConvertersProvider {
    @Override
    public Date convert(Object val) throws ConverterException {

        // Every oracle date and timestamp type extends Datum, not just TIMESTAMP: the zone aware ones,
        // TIMESTAMPTZ and TIMESTAMPLTZ, are siblings of it. Checking for TIMESTAMP alone left them to the
        // base converter, which cannot read them and fails with "Cannot convert type class ...".
        if (val instanceof Datum) {
            try {
                return ((Datum)val).timestampValue();
            } catch (SQLException e) {
                throw new ConverterException(
                        "Error trying to convert " + val.getClass().getName() + " to java.util.Date", e);
            }
        }

        return super.convert(val);
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(Date.class,  new OracleDateConverter());
    }
}
