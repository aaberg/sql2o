package org.sql2o.converters;

import java.util.UUID;

import org.sql2o.tools.BigEndian;

/**
 * A uuid kept as the sixteen bytes of a binary column, which is what a database without a uuid type stores it as.
 *
 * <p>The sixteen bytes are the two {@code long}s a uuid is made of, most significant byte first, laid end to end and
 * assembled and taken apart through {@link BigEndian} so that nothing is allocated for the sake of
 * shifting eight bytes around. What the column is called on a given database is that database's business and is written
 * down in the extension that carries it.
 *
 * <p><b>Not registered by default.</b> {@link Convert} leaves it alone on purpose: only a driver that actually stores a
 * uuid this way should give it out, and it does so by registering an instance of this class in its own quirks, where
 * {@link org.sql2o.quirks.NoQuirks#converterOf(Class)} looks before it looks at the global registry. A driver that
 * stores a uuid some other way, as text or as a type of its own, has nothing to gain from this and would be worse off
 * with it, since a driver asked to place a {@link UUID} as the object itself is free to do better.
 *
 * @see org.sql2o.quirks.Db2Quirks#setParameter(java.sql.PreparedStatement, int, UUID)
 */
public class BinaryUUIDConverter extends UUIDConverter {

    /** How many bytes a uuid takes, which is the width of the column it goes in. */
    public static final int LENGTH_OF_UUID = 16;

    @Override
    public UUID convert(Object val) throws ConverterException {
        if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;

            if (bytes.length != LENGTH_OF_UUID) {
                // Sixteen bytes are the whole of a uuid, so an array of another length is not one. Saying so is better
                // than reading past the end of it.
                throw new ConverterException("Cannot read a UUID from " + bytes.length + " bytes, it takes " + LENGTH_OF_UUID);
            }

            return new UUID(BigEndian.readLong(bytes, 0),
                    BigEndian.readLong(bytes, 8));
        } else {
            return super.convert(val);
        }
    }

    @Override
    public Object toDatabaseParam(UUID val) {
        byte[] bytes = new byte[LENGTH_OF_UUID];

        BigEndian.writeLong(bytes, 0, val.getMostSignificantBits());
        BigEndian.writeLong(bytes, 8, val.getLeastSignificantBits());

        return bytes;
    }
}