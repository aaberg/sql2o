package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.Db2UUIDConverter;
import org.sql2o.converters.DefaultConverter;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link Db2Quirks}.
 *
 * <p>Two things are pinned here. The first is what the subclass must not change: db2 does not deviate from
 * {@link NoQuirks} about column naming, the date and time handling of a driver lives in the converters of core, and a
 * converter map handed to the constructor wins over everything. The second is the one thing it does carry: db2 has no
 * uuid type, so a uuid is stored as the sixteen bytes of its two longs, and both the typed and the untyped call have to
 * arrive at those bytes rather than at the object.
 */
public class Db2QuirksTest {

    private static final String A_UUID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

    /** The sixteen bytes that uuid is, as the database sees them. */
    private static final String ITS_SIXTEEN_BYTES = "6BA7B8109DAD11D180B400C04FD430C8";

    @Test
    public void theColumnIsNamedByItsAliasAndNotByItsUnderlyingName() throws SQLException {
        assertEquals("MY_ID", new Db2Quirks().getColumnName(meta(), 1));
    }

    /** The same stub read through the default quirks has to give the same answer, since db2 does not deviate. */
    @Test
    public void db2AgreesWithTheDefaultAboutColumnNames() throws SQLException {
        ResultSetMetaData meta = meta();

        assertEquals(new NoQuirks().getColumnName(meta, 1), new Db2Quirks().getColumnName(meta, 1));
    }

    @Test
    public void aLocalConverterIsUsed() {
        final Converter own = new DefaultConverter();
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(String.class, own);

        assertSame(own, new Db2Quirks(converters).converterOf(String.class));
    }

    /** The inherited behaviour has to keep working through this subclass as well. */
    @Test
    public void theDefaultsOfNoQuirksStillApply() throws SQLException {
        final Db2Quirks quirks = new Db2Quirks();

        assertEquals("MY_ID", quirks.getColumnName(meta(), 1));
        assertEquals(org.sql2o.converters.Convert.getConverterIfExists(String.class), quirks.converterOf(String.class));
    }

    @Test
    public void aUuidIsWrittenAsTheSixteenBytesOfItsTwoLongs() throws SQLException {
        byte[] written = bytesWrittenBy(statement ->
                new Db2Quirks().setParameter(statement, 1, UUID.fromString(A_UUID)));

        assertEquals(16, written.length);
        assertEquals(ITS_SIXTEEN_BYTES, hexOf(written));
    }

    /**
     * The typed overload of {@code addParameter} has no branch for a uuid, so it hands the value over as an object, and
     * this is the branch that turns it into bytes. Were it lost, db2 would be handed the uuid itself and refuse the
     * conversion with SQLCODE -4461, which is what the statement proxy below makes visible by failing on anything else.
     */
    @Test
    public void aUuidIsRoutedToThoseBytesFromTheObjectOverload() throws SQLException {
        byte[] written = bytesWrittenBy(statement ->
                new Db2Quirks().setParameter(statement, 1, (Object) UUID.fromString(A_UUID)));

        assertEquals(ITS_SIXTEEN_BYTES, hexOf(written));
    }

    /** The parent would answer with the converter of core, which cannot read bytes and throws on them. */
    @Test
    public void theUuidConverterIsRegisteredForUuid() {
        assertInstanceOf(Db2UUIDConverter.class, new Db2Quirks().converterOf(UUID.class));
    }

    @Test
    public void theUuidConverterReadsThoseBytesBack() throws Exception {
        Converter<UUID> converter = new Db2Quirks().converterOf(UUID.class);

        UUID read = converter.convert(bytesOf(ITS_SIXTEEN_BYTES));

        assertEquals(UUID.fromString(A_UUID), read);
    }

    /**
     * Metadata that answers differently depending on whether the label or the name is asked for, which is what makes
     * the difference visible: the label is the alias the query gave, the name is the column underneath it.
     *
     * <p>A proxy keeps this to a few lines, where implementing the interface would run to two hundred.
     */
    private static ResultSetMetaData meta() {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                Db2QuirksTest.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getColumnLabel":
                            return "MY_ID";
                        case "getColumnName":
                            return "ID";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** What a binding did to a statement, where a statement is only ever written to. */
    private interface Binding {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /**
     * The bytes a binding handed over, or rather whether it did: the proxy refuses every other method, so a binding that
     * reached for anything but {@code setBytes} fails here instead of passing a test it never deserved.
     */
    private static byte[] bytesWrittenBy(Binding binding) {
        AtomicReference<byte[]> written = new AtomicReference<>();
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                Db2QuirksTest.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setBytes".equals(method.getName())) {
                        written.set((byte[]) args[1]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        try {
            binding.bind(statement);
        } catch (SQLException e) {
            throw new AssertionError("a statement that only records what it is given cannot fail", e);
        }

        return written.get();
    }

    private static byte[] bytesOf(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static String hexOf(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }
}