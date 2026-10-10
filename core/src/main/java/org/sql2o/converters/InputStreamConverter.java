package org.sql2o.converters;

import java.io.ByteArrayInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 6/13/13
 * Time: 11:40 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Converts whatever {@link ByteArrayConverter} accepts into a stream over the same bytes.
 *
 * <p>Note that the wrapping is not complete: only a {@link ConverterException} from the byte array converter is
 * turned into one here, while the bare {@link RuntimeException} it throws for an unsupported type passes through
 * unchanged. Callers therefore have to handle both. See docs/converter-exceptions.md.
 */
public class InputStreamConverter extends ConverterBase<ByteArrayInputStream> {
    public ByteArrayInputStream convert(Object val) throws ConverterException {
        if (val == null) return null;

        try {
            return new ByteArrayInputStream( new ByteArrayConverter().convert(val) );
        } catch( ConverterException e) {
            throw new ConverterException("Error converting Blob to InputSteam");
        }
    }
}
