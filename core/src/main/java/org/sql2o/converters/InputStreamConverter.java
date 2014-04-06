package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 6/13/13
 * Time: 11:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputStreamConverter extends BuiltInConverterBase<ByteArrayInputStream> {
    public ByteArrayInputStream convert(Object val) throws ConverterException {
        if (val == null) return null;

        try {
            return new ByteArrayInputStream( new ByteArrayConverter().convert(val) );
        } catch( ConverterException e) {
            throw new ConverterException("Error converting Blob to InputSteam");
        }
    }

    public void addParameter(StatementParameterSetter stmt, String name, ByteArrayInputStream val) throws SQLException {
        stmt.setObject(name, val);
    }
}
