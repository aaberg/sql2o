package org.sql2o.tools;

/**
 * Reads and writes a long as eight bytes, most significant byte first.
 *
 * <p>The order is big endian, which is the one {@link java.nio.ByteBuffer} uses by default and the one the binary columns
 * of every database sql2o supports store their bytes in. It is in the name rather than in a comment because getting it
 * wrong is invisible: a value written the wrong way round still reads back correctly through the same pair of methods, so
 * nothing fails and the bytes in the column simply are not the bytes anyone meant. Any other order would be a change to
 * what is already stored, and therefore a migration rather than a fix.
 *
 * <p>This is what the converters that keep a value as bytes are written in terms of, a {@link java.util.UUID} stored in a
 * binary column being the case they all came from. {@link BigEndian#writeLong(byte[], int, long)} allocates nothing beyond
 * the array it is given, which is the one the caller wanted written into anyway; doing the same through a buffer would
 * allocate a buffer for every value converted, and the bytes shifted around are not worth that.
 *
 * <p>Stateless, and therefore safe to use from anywhere including several threads at once.
 *
 * <pre>{@code
 * byte[] sixteen = new byte[16];
 * BigEndian.writeLong(sixteen, 0, uuid.getMostSignificantBits());
 * BigEndian.writeLong(sixteen, 8, uuid.getLeastSignificantBits());
 *
 * UUID back = new UUID(BigEndian.readLong(sixteen, 0), BigEndian.readLong(sixteen, 8));
 * }</pre>
 */
public final class BigEndian {

    /** How many bytes a long takes, and therefore how much room a buffer needs at an offset. */
    public static final int SIZE_OF_LONG = 8;

    private BigEndian() {
        // Nothing in here is of an instance.
    }

    /**
     * Puts a long into the eight bytes of a buffer at an offset, most significant byte first.
     *
     * <p>The bytes outside those eight are left as they were, which is what makes writing at an offset worth having: a
     * sixteen byte value is two longs in one array rather than two arrays and a copy.
     *
     * @param buffer the bytes to write into
     * @param offset where the eight bytes start
     * @param value what to write
     * @throws NullPointerException if the buffer is null
     * @throws IndexOutOfBoundsException if fewer than eight bytes are left at the offset
     */
    public static void writeLong(byte[] buffer, int offset, long value) {
        checkRoom(buffer, offset);

        for (int i = offset + SIZE_OF_LONG - 1; i >= offset; i--) {
            buffer[i] = (byte) value;
            value >>>= 8;
        }
    }

    /**
     * Reads the long in the eight bytes of a buffer at an offset, most significant byte first.
     *
     * <p>Each byte is masked to eight bits before it goes into the result, since a {@code byte} is signed and the top
     * half of a long is routinely {@code 0xFF} rather than {@code 0x00}.
     *
     * @param buffer the bytes to read from
     * @param offset where the eight bytes start
     * @return what they say
     * @throws NullPointerException if the buffer is null
     * @throws IndexOutOfBoundsException if fewer than eight bytes are left at the offset
     */
    public static long readLong(byte[] buffer, int offset) {
        checkRoom(buffer, offset);

        long value = 0;

        for (int i = offset; i < offset + SIZE_OF_LONG; i++) {
            value = (value << 8) | (buffer[i] & 0xFFL);
        }

        return value;
    }

    private static void checkRoom(byte[] buffer, int offset) {
        if (buffer == null) {
            throw new NullPointerException("there is no buffer to read from or write to");
        }

        // Compared against the end of the buffer rather than as offset + 8 > length, which would let an offset near
        // Integer.MAX_VALUE wrap around into a range that looks valid and fail later, inside the loop, as an
        // ArrayIndexOutOfBoundsException about an index nobody wrote.
        if (offset < 0 || offset > buffer.length - SIZE_OF_LONG) {
            throw new IndexOutOfBoundsException("a long needs " + SIZE_OF_LONG + " bytes at offset " + offset
                    + ", and the buffer holds " + buffer.length);
        }
    }
}