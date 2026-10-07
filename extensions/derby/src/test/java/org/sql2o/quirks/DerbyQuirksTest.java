package org.sql2o.quirks;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.converters.DefaultConverter;
import org.sql2o.converters.DerbyUUIDConverter;
import org.sql2o.converters.InstantConverter;
import org.sql2o.converters.InstantToTimestampConverter;
import org.sql2o.converters.LocalDateConverter;
import org.sql2o.converters.LocalDateTimeConverter;
import org.sql2o.converters.LocalDateTimeToTimestampConverter;
import org.sql2o.converters.LocalDateToSqlDateConverter;
import org.sql2o.converters.LocalTimeConverter;
import org.sql2o.converters.LocalTimeToSqlTimeConverter;
import org.sql2o.converters.OffsetDateTimeConverter;
import org.sql2o.converters.OffsetDateTimeToTimestampConverter;
import org.sql2o.converters.OffsetTimeConverter;
import org.sql2o.converters.OffsetTimeToTimeConverter;
import org.sql2o.converters.StringConverter;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link DerbyQuirks}.
 *
 * <p>Two things are pinned here. The first is what the subclass must not change: derby does not deviate from
 * {@link NoQuirks} about column naming, and a converter map handed to the constructor wins over everything. The second is
 * the two things it does carry: derby has no uuid type, so a uuid is stored as the sixteen bytes a for bit data column
 * holds, and its driver predates {@code java.time}, so every one of those types is converted on the way in rather than
 * handed to the driver as it is.
 */
public class DerbyQuirksTest {

    private static final String A_UUID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

    /** The sixteen bytes that uuid is, as the database sees them. */
    private static final String ITS_SIXTEEN_BYTES = "6BA7B8109DAD11D180B400C04FD430C8";

    @Test
    public void theColumnIsNamedByItsAliasAndNotByItsUnderlyingName() throws SQLException {
        assertEquals("MY_ID", new DerbyQuirks().getColumnName(meta(), 1));
    }

    /** The same stub read through the default quirks has to give the same answer, since derby does not deviate. */
    @Test
    public void derbyAgreesWithTheDefaultAboutColumnNames() throws SQLException {
        ResultSetMetaData meta = meta();

        assertEquals(new NoQuirks().getColumnName(meta, 1), new DerbyQuirks().getColumnName(meta, 1));
    }

    @Test
    public void aLocalConverterIsUsed() {
        final Converter own = new DefaultConverter();
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(String.class, own);

        assertSame(own, new DerbyQuirks(converters).converterOf(String.class));
    }

    /** The inherited behaviour has to keep working through this subclass as well. */
    @Test
    public void theDefaultsOfNoQuirksStillApply() throws SQLException {
        final DerbyQuirks quirks = new DerbyQuirks();

        assertEquals("MY_ID", quirks.getColumnName(meta(), 1));
        assertEquals(org.sql2o.converters.Convert.getConverterIfExists(String.class), quirks.converterOf(String.class));
    }

    @Test
    public void aUuidIsWrittenAsTheSixteenBytesOfItsTwoLongs() throws SQLException {
        byte[] written = bytesWrittenBy(statement ->
                new DerbyQuirks().setParameter(statement, 1, UUID.fromString(A_UUID)));

        assertEquals(16, written.length);
        assertEquals(ITS_SIXTEEN_BYTES, hexOf(written));
    }

    /**
     * The typed overload of {@code addParameter} has no branch for a uuid, so it hands the value over as an object, and
     * this is the branch that turns it into bytes. Were it lost, the value would reach the driver as the uuid itself.
     */
    @Test
    public void aUuidIsRoutedToThoseBytesFromTheObjectOverload() throws SQLException {
        byte[] written = bytesWrittenBy(statement ->
                new DerbyQuirks().setParameter(statement, 1, (Object) UUID.fromString(A_UUID)));

        assertEquals(ITS_SIXTEEN_BYTES, hexOf(written));
    }

    /** The parent would answer with the converter of core, which cannot read bytes and throws on them. */
    @Test
    public void theUuidConverterIsRegisteredForUuid() {
        assertInstanceOf(DerbyUUIDConverter.class, new DerbyQuirks().converterOf(UUID.class));
    }

    @Test
    public void theUuidConverterReadsThoseBytesBack() throws Exception {
        Converter<UUID> converter = new DerbyQuirks().converterOf(UUID.class);

        UUID read = converter.convert(bytesOf(ITS_SIXTEEN_BYTES));

        assertEquals(UUID.fromString(A_UUID), read);
    }

    /**
     * The driver takes none of the {@code java.time} types, so each of them has a converter that writes the
     * {@code java.sql} one instead. They are registered by type here rather than reached for, since a registration that
     * went missing would only show up as a {@code SQLDataException} from the driver at run time.
     */
    @Test
    public void everyJavaTimeTypeIsRegisteredWithTheConverterThatWritesTheJavaSqlOne() {
        DerbyQuirks quirks = new DerbyQuirks();

        assertInstanceOf(InstantToTimestampConverter.class, quirks.converterOf(Instant.class));
        assertInstanceOf(OffsetDateTimeToTimestampConverter.class, quirks.converterOf(OffsetDateTime.class));
        assertInstanceOf(OffsetTimeToTimeConverter.class, quirks.converterOf(OffsetTime.class));
        assertInstanceOf(LocalDateToSqlDateConverter.class, quirks.converterOf(LocalDate.class));
        assertInstanceOf(LocalTimeToSqlTimeConverter.class, quirks.converterOf(LocalTime.class));
        assertInstanceOf(LocalDateTimeToTimestampConverter.class, quirks.converterOf(LocalDateTime.class));
    }

    /** What each of them writes, since the point of registering them is the value the driver is handed. */
    @Test
    public void theJavaTimeTypesAreWrittenAsTheJavaSqlOnes() {
        DerbyQuirks quirks = new DerbyQuirks();

        assertEquals(Timestamp.from(Instant.parse("2020-01-01T09:34:56.789Z")),
                quirks.converterOf(Instant.class).toDatabaseParam(Instant.parse("2020-01-01T09:34:56.789Z")));
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2020, 1, 1)),
                quirks.converterOf(LocalDate.class).toDatabaseParam(LocalDate.of(2020, 1, 1)));
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 12, 34, 56)),
                quirks.converterOf(LocalDateTime.class).toDatabaseParam(LocalDateTime.of(2020, 1, 1, 12, 34, 56)));
        assertEquals(java.sql.Time.valueOf(LocalTime.of(12, 34, 56)),
                quirks.converterOf(LocalTime.class).toDatabaseParam(LocalTime.of(12, 34, 56)));
    }

    /** A type derby has no trouble with is left to the converter of core, rather than being written as something else. */
    @Test
    public void theTypesTheDriverTakesAreTheOnesOfCore() {
        DerbyQuirks quirks = new DerbyQuirks();

        assertInstanceOf(StringConverter.class, quirks.converterOf(String.class));
        assertInstanceOf(org.sql2o.converters.IntegerConverter.class, quirks.converterOf(Integer.class));
        assertInstanceOf(org.sql2o.converters.BigDecimalConverter.class, quirks.converterOf(java.math.BigDecimal.class));
    }

    /**
     * The registered converters have to be the ones of core extended rather than copies, since the read side is
     * inherited and a copy of it would be one more thing to keep in step with no reason.
     */
    @Test
    public void theRegisteredConvertersExtendTheOnesOfCore() {
        DerbyQuirks quirks = new DerbyQuirks();

        assertInstanceOf(InstantConverter.class, quirks.converterOf(Instant.class));
        assertInstanceOf(OffsetDateTimeConverter.class, quirks.converterOf(OffsetDateTime.class));
        assertInstanceOf(OffsetTimeConverter.class, quirks.converterOf(OffsetTime.class));
        assertInstanceOf(LocalDateConverter.class, quirks.converterOf(LocalDate.class));
        assertInstanceOf(LocalTimeConverter.class, quirks.converterOf(LocalTime.class));
        assertInstanceOf(LocalDateTimeConverter.class, quirks.converterOf(LocalDateTime.class));
    }

    /**
     * Metadata that answers differently depending on whether the label or the name is asked for, which is what makes
     * the difference visible: the label is the alias the query gave, the name is the column underneath it.
     *
     * <p>A proxy keeps this to a few lines, where implementing the interface would run to two hundred.
     */
    private static ResultSetMetaData meta() {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                DerbyQuirksTest.class.getClassLoader(),
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
                DerbyQuirksTest.class.getClassLoader(),
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