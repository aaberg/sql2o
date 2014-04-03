package org.sql2o.converters;

import org.sql2o.tools.IOUtils;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 6/13/13
 * Time: 11:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ByteArrayConverter implements Converter<byte[]> {
    public byte[] convert(Object val) throws ConverterException {
        if (val == null) return null;

        if (val instanceof Blob) {
            Blob b = (Blob)val;
            try {
                return IOUtils.toByteArray( b.getBinaryStream() );
            } catch (SQLException e) {
                throw new ConverterException("Error converting Blob to byte[]", e);
            } catch (IOException e) {
                throw new ConverterException("Error converting Blob to byte[]", e);
            }
        }

        if (val instanceof byte[]){
            return (byte[])val;
        }

        throw new RuntimeException("could not convert " + val.getClass().getName() + " to byte[]");
    }
}
