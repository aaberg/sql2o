package org.sql2o.converters;

import org.sql2o.tools.BigEndian;

import java.util.UUID;

/**
 * DB2 has no uuid type, so a uuid is kept as the sixteen bytes of a {@code VARCHAR(n) FOR BIT DATA} column and turned
 * back into a {@link UUID} here.
 *
 * <p>Db2 hands that column back as a {@code byte[]}, which the {@link UUIDConverter} of core cannot read, so this adds
 * the bytes back together as the two longs a uuid is made of. Anything else it is given is left to the parent, which
 * already knows a uuid and the text form of one.
 *
 * <p>Which column to use is not a free choice, and it was measured against DB2 11.5 rather than reasoned about:
 * {@code CHAR(16) FOR BIT DATA} and {@code VARCHAR(16) FOR BIT DATA} both arrive as bytes and are equivalent for a
 * uuid, since a uuid is always sixteen bytes and the blank padding of the {@code CHAR} form never comes into play;
 * {@code BINARY(16)} also arrives as bytes but only exists from DB2 11.5 on, which would tie the column to a driver
 * version for no gain.
 *
 * @see org.sql2o.quirks.Db2Quirks#setParameter(java.sql.PreparedStatement, int, UUID)
 */
public class Db2UUIDConverter extends UUIDConverter {

    @Override
    public UUID convert(Object val) throws ConverterException {
        if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;

            if (bytes.length != 16) {
                // Sixteen bytes are the whole of a uuid, so an array of another length is not one. Saying so is better
                // than reading past the end of it.
                throw new ConverterException("Cannot read a UUID from " + bytes.length + " bytes, it takes 16");
            }

            return new UUID(BigEndian.readLong(bytes, 0), BigEndian.readLong(bytes, 8));
        } else {
            return super.convert(val);
        }
    }

    @Override
    public Object toDatabaseParam(UUID val) {
        byte[] bytes = new byte[16];

        BigEndian.writeLong(bytes, 0, val.getMostSignificantBits());
        BigEndian.writeLong(bytes, 8, val.getLeastSignificantBits());

        return bytes;
    }
}