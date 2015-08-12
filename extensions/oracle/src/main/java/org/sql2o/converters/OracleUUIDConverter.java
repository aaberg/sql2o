package org.sql2o.converters;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by lars on 12.08.15.
 */
public class OracleUUIDConverter implements Converter<UUID> {

    private final UUIDConverter baseConverter = new UUIDConverter();

    @Override
    public UUID convert(Object val) throws ConverterException {
        if (val instanceof byte[]) {
            ByteBuffer bb = ByteBuffer.wrap((byte[])val);

            long mostSignigcant = bb.getLong();
            long leastSignificant = bb.getLong();

            return new UUID(mostSignigcant, leastSignificant);
        } else {
            return baseConverter.convert(val);
        }
    }

    @Override
    public Object toDatabaseParam(UUID val) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(val.getMostSignificantBits());
        bb.putLong(val.getLeastSignificantBits());
        return bb.array();
    }
}
