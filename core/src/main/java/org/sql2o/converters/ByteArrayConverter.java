package org.sql2o.converters;

import org.sql2o.tools.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * User: lars
 * Date: 6/13/13
 * Time: 11:36 PM
 */
/**
 * Reads a value from the database as a byte array, either from a {@link Blob} or from an array that is already in
 * the right shape.
 *
 * <p>Unlike the rest of this package, an unsupported type is refused with a bare {@link RuntimeException} rather than
 * a {@link ConverterException}, even though the method declares the checked one. Callers that want to handle every
 * failure the same way have to catch both, and {@link InputStreamConverter}, which delegates here, lets the runtime
 * one escape. See docs/converter-exceptions.md.
 */
public class ByteArrayConverter extends ConverterBase<byte[]> {

    public byte[] convert(Object val) throws ConverterException {
        if (val == null) return null;

        if (val instanceof Blob) {
            Blob b = (Blob)val;
            InputStream stream=null;
            try {
                try {
                    stream = b.getBinaryStream();
                    return IOUtils.toByteArray(stream);
                } finally {
                    if(stream!=null) {
                        try {
                            stream.close();
                        } catch (Throwable ignore){
                            // ignore stream.close errors
                        }
                    }
                    try {
                        b.free();
                    } catch (Throwable ignore){
                        // ignore blob.free errors
                    }
                }
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
