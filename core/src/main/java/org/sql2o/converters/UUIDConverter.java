package org.sql2o.converters;

import org.sql2o.tools.StatementParameterSetter;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Used by sql2o to convert a value from the database into a {@link UUID}.
 */
public class UUIDConverter extends BuiltInConverterBase<UUID> {

    public UUID convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (UUID.class.isAssignableFrom( val.getClass() )){
            return (UUID)val;
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.UUID");
    }

    public void addParameter(StatementParameterSetter stmt, String name, UUID val) throws SQLException {
        stmt.setObject(name, val);
    }
}

