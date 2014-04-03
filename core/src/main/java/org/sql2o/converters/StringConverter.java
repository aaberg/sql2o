package org.sql2o.converters;

import java.sql.Clob;
import java.sql.SQLException;

/**
 * Used by sql2o to convert a value from the database into a {@link String}.
 */
public class StringConverter implements Converter<String>{

    public String convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (val instanceof Clob) {
            Clob clobVal = (Clob)val;
            try {
                return clobVal.getSubString(1, (int)clobVal.length());
            } catch (SQLException e) {
                throw new ConverterException("error converting clob to String", e);
            }
        }

        return val.toString().trim();
    }
}
