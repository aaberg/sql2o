package org.sql2o.converters.joda;

import oracle.sql.Datum;
import org.joda.time.LocalDate;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.converters.ConvertersProvider;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by lars on 01.05.14.
 */
public class OracleLocalDateConverter extends JodaLocalDateConverter implements ConvertersProvider {
    @Override
    public LocalDate convert(Object val) throws ConverterException {

        if (val instanceof Datum){
            try {
                return new LocalDate (((Datum)val).dateValue());
            } catch (SQLException e) {
                throw new ConverterException(String.format("Error trying to convert oracle date to %s", LocalDate.class.getName()), e);
            }

        }

        return super.convert(val);
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(LocalDate.class, new OracleLocalDateConverter());
    }
}
