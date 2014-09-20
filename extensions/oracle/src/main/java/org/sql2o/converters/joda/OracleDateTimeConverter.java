package org.sql2o.converters.joda;

import oracle.sql.Datum;
import org.joda.time.DateTime;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.converters.ConvertersProvider;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by lars on 01.05.14.
 */
public class OracleDateTimeConverter extends DateTimeConverter implements ConvertersProvider{

    @Override
    public DateTime convert(Object val) throws ConverterException {

        if (val instanceof Datum) {
            try {
                return new DateTime(((Datum)val).timestampValue());
            } catch (SQLException e) {
                throw new ConverterException(String.format("Error trying to convert oracle timestamp to %s", DateTime.class.getName()), e);
            }
        }

        return super.convert(val);
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> mapToFill) {
        mapToFill.put(DateTime.class, new OracleDateTimeConverter());
    }
}
