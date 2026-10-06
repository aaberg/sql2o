package org.sql2o.converters;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The contract of a converter that keeps a uuid as the sixteen bytes of a binary column, which is what a database
 * without a uuid type needs.
 *
 * <p>The bytes have to be the ones a {@link ByteBuffer} would have written, since that is how they were written before
 * this was done by hand, and a uuid already sitting in a column of the old shape has to keep reading back as itself. So
 * the buffer stays here as the thing to compare against, even though the converters no longer use it: it is the
 * definition of the layout rather than part of the code under test.
 *
 * <p>Every extension that has one of these converters runs this against its own, the same way the typed parameter matrix
 * is shared between the databases.
 */
public abstract class BinaryUuidConverterTest {

    /** An ordinary one, the shape a uuid out of a database usually has. */
    private static final UUID A_UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    /** All zeros, where a shift that loses a byte still gives all zeros back. */
    private static final UUID ALL_ZEROES = new UUID(0L, 0L);

    /** Every bit set, so that a byte read as signed would show up as a wrong value. */
    private static final UUID ALL_ONES = new UUID(-1L, -1L);

    /** The top bit of each half set and nothing else, which is where a byte order mistake shows first. */
    private static final UUID TOP_BITS_ONLY = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);

    private static final UUID[] SOME_UUIDS = {A_UUID, ALL_ZEROES, ALL_ONES, TOP_BITS_ONLY};

    /** The converter under test, which is the only thing a subclass has to supply. */
    protected abstract Converter<UUID> converter();

    @Test
    public void aUuidIsWrittenAsSixteenBytes() {
        byte[] written = (byte[]) converter().toDatabaseParam(A_UUID);

        assertEquals(16, written.length);
    }

    /**
     * The identity of the conversion: byte for byte what a buffer would have written, so that what is already stored in a
     * column of the old shape keeps meaning the same thing.
     */
    @Test
    public void theBytesAreExactlyWhatAByteBufferWouldHaveWritten() {
        for (UUID uuid : SOME_UUIDS) {
            assertArrayEquals(referenceBytes(uuid), (byte[]) converter().toDatabaseParam(uuid),
                    "the bytes of " + uuid);
        }
    }

    /** And the same the other way round, which is what reading a column actually goes through. */
    @Test
    public void aUuidIsReadBackFromThoseBytes() throws ConverterException {
        for (UUID uuid : SOME_UUIDS) {
            assertEquals(uuid, converter().convert(referenceBytes(uuid)),
                    "the uuid read back from the bytes of " + uuid);
        }
    }

    @Test
    public void aUuidSurvivesTheRoundTrip() throws ConverterException {
        for (UUID uuid : SOME_UUIDS) {
            assertEquals(uuid, converter().convert((byte[]) converter().toDatabaseParam(uuid)),
                    "the uuid after a round trip through " + uuid);
        }
    }

    /** An array of another length is not a uuid, and saying so beats reading past the end of it. */
    @Test
    public void bytesOfTheWrongLengthAreRefusedRatherThanRead() {
        Converter<UUID> converter = converter();

        assertThrows(ConverterException.class, () -> converter.convert(new byte[15]));
        assertThrows(ConverterException.class, () -> converter.convert(new byte[17]));
        assertThrows(ConverterException.class, () -> converter.convert(new byte[0]));
    }

    /** The parent still answers for what it always did: a uuid, and the text form of one. */
    @Test
    public void aUuidOrItsTextIsStillReadByTheParent() throws ConverterException {
        Converter<UUID> converter = converter();

        assertEquals(A_UUID, converter.convert(A_UUID));
        assertEquals(A_UUID, converter.convert(A_UUID.toString()));
        assertNull(converter.convert(null));
    }

    /**
     * The bytes as a {@link ByteBuffer} writes them, which is big endian and therefore the most significant byte first.
     * Deliberately the obvious implementation rather than a copy of the one under test, or the test would only be saying
     * that the code agrees with itself.
     */
    private static byte[] referenceBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        return buffer.array();
    }
}