package org.sql2o.converters;

import org.sql2o.tools.BigEndian;

import java.util.UUID;

/**
 * HSQLDB has no uuid type, so a uuid is kept as the sixteen bytes of a {@code binary(16)} column and turned back into a
 * {@link UUID} here.
 *
 * <p>HSQLDB hands that column back as a {@code byte[]}, which the {@link UUIDConverter} of core cannot read, so this
 * adds the bytes back together as the two longs a uuid is made of. Anything else it is given is left to the parent, which
 * already knows a uuid and the text form of one.
 *
 * <p>Which column to use is not a free choice, and it was measured against HSQLDB 2.7 rather than reasoned about:
 * {@code binary(16)}, {@code varbinary(16)} and {@code longvarbinary} all arrive as bytes, and a uuid is always exactly
 * sixteen bytes, so the fixed length of the first one never has to pad anything. {@code varchar} and {@code char} arrive
 * as a {@link String} instead, which would mean storing the dashed form as text and reading it back as text.
 *
 * @see org.sql2o.quirks.HsqlQuirks#setParameter(java.sql.PreparedStatement, int, UUID)
 */
public class HsqlUUIDConverter extends UUIDConverter {

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