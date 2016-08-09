package org.sql2o.converters.joda;

import oracle.sql.Datum;
import org.joda.time.LocalTime;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.converters.ConvertersProvider;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by lars on 01.05.14.
 */
public class OracleLocalTimeConverter extends LocalTimeConverter implements ConvertersProvider {

    @Override
    public LocalTime convert(Object val) throws ConverterException {

        if (val instanceof Datum){
            try {
                return new LocalTime (((Datum)val).timestampValue());
            } catch (SQLException e) {
                throw new ConverterException(String.format("Error trying to convert oracle time to %s", LocalTime.class.getName()), e);
            }
        }

        return super.convert(val);
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(LocalTime.class, new OracleLocalTimeConverter());
    }
}
