package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link BigEndian}, which every converter that keeps a value as bytes is written in terms of.
 *
 * <p>The order of the bytes is the one thing here that cannot quietly change, since a value written with the other one
 * still reads back as itself through the same pair of methods, and would only be noticed by whoever queried the column
 * afterwards. So the reference is {@link ByteBuffer}, which is the definition of the layout and is deliberately not
 * what the code under test uses.
 */
public class BigEndianTest {

    private static final long[] VALUES = {
            0L,
            1L,
            -1L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            0x0102030405060708L,
            -0x0102030405060708L,
            0x6ba7b8109dad11d1L,
            0x80b400c04fd430c8L
    };

    private static byte[] reference(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(BigEndian.SIZE_OF_LONG);
        buffer.putLong(value);

        return buffer.array();
    }

    @Test
    public void aLongIsEightBytes() {
        assertEquals(8, BigEndian.SIZE_OF_LONG);
    }

    @Test
    public void aLongIsWrittenTheWayAByteBufferWouldWriteIt() {
        for (long value : VALUES) {
            byte[] written = new byte[BigEndian.SIZE_OF_LONG];

            BigEndian.writeLong(written, 0, value);

            assertArrayEquals(reference(value), written, "the bytes of " + value);
        }
    }

    @Test
    public void aLongIsReadTheWayAByteBufferWouldHaveWrittenIt() {
        for (long value : VALUES) {
            assertEquals(value, BigEndian.readLong(reference(value), 0), "the value of " + value);
        }
    }

    @Test
    public void aLongSurvivesTheRoundTrip() {
        for (long value : VALUES) {
            byte[] written = new byte[BigEndian.SIZE_OF_LONG];

            BigEndian.writeLong(written, 0, value);

            assertEquals(value, BigEndian.readLong(written, 0), "the value of " + value);
        }
    }

    /** Both halves of a sixteen byte value, which is how a uuid is written, and where an offset actually gets used. */
    @Test
    public void twoLongsLandAtTheTwoOffsetsOfASixteenByteValue() {
        byte[] sixteen = new byte[2 * BigEndian.SIZE_OF_LONG];

        BigEndian.writeLong(sixteen, 0, 0x6ba7b8109dad11d1L);
        BigEndian.writeLong(sixteen, 8, 0x80b400c04fd430c8L);

        assertEquals("6BA7B8109DAD11D180B400C04FD430C8", hexOf(sixteen));
        assertEquals(0x6ba7b8109dad11d1L, BigEndian.readLong(sixteen, 0));
        assertEquals(0x80b400c04fd430c8L, BigEndian.readLong(sixteen, 8));
    }

    /** Writing leaves the rest of the buffer alone, which is what makes writing at an offset worth having. */
    @Test
    public void writingAtAnOffsetTouchesNothingElse() {
        byte[] buffer = new byte[16];
        java.util.Arrays.fill(buffer, (byte) 0x7F);

        BigEndian.writeLong(buffer, 8, 0L);

        assertEquals("7F7F7F7F7F7F7F7F0000000000000000", hexOf(buffer));
    }

    @Test
    public void anOffsetWithTooFewBytesLeftIsRefused() {
        byte[] buffer = new byte[7];

        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.writeLong(buffer, 0, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.readLong(buffer, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.writeLong(buffer, -1, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.readLong(buffer, 1));
    }

    /**
     * A negative offset, and an offset so large that {@code offset + 8} would wrap around into a valid range if it were
     * the way the check were written.
     */
    @Test
    public void anOffsetOutsideTheBufferIsRefusedRatherThanWrappingAroundIt() {
        byte[] buffer = new byte[16];

        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.writeLong(buffer, -1, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.readLong(buffer, -8));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.writeLong(buffer, 9, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.readLong(buffer, 16));
        assertThrows(IndexOutOfBoundsException.class, () -> BigEndian.writeLong(buffer, Integer.MAX_VALUE, 1L));    }

    @Test
    public void noBufferIsRefusedRatherThanReportedAsAnOffset() {
        assertThrows(NullPointerException.class, () -> BigEndian.writeLong(null, 0, 1L));
        assertThrows(NullPointerException.class, () -> BigEndian.readLong(null, 0));
    }

    /** Exactly eight bytes left is room enough, and not one byte more. */
    @Test
    public void exactlyEightBytesAreRoomEnough() {
        byte[] buffer = new byte[8];

        BigEndian.writeLong(buffer, 0, Long.MIN_VALUE);

        assertEquals("8000000000000000", hexOf(buffer));
        assertEquals(Long.MIN_VALUE, BigEndian.readLong(buffer, 0));
    }

    private static String hexOf(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }

        return hex.toString();
    }
}