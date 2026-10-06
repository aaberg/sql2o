package org.sql2o.converters;

import org.sql2o.tools.BigEndian;

import java.util.UUID;

/**
 * Oracle has no uuid type, so a uuid is kept as the sixteen bytes of a {@code RAW(16)} column and turned back into a
 * {@link UUID} here.
 *
 * <p>Anything else it is given is left to the parent, which already knows a uuid and the text form of one.
 *
 * <p>Which column to use is not a free choice: {@code RAW(16)} arrives as bytes, where a {@code VARCHAR2} would arrive as
 * the text of the dashed form and have to be parsed back.
 *
 * @see org.sql2o.quirks.OracleQuirks#setParameter(java.sql.PreparedStatement, int, UUID)
 */
public class OracleUUIDConverter extends UUIDConverter {

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