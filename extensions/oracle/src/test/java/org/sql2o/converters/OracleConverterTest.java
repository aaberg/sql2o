package org.sql2o.converters;

import org.junit.Test;
import org.sql2o.quirks.OracleQuirks;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by lars on 01.05.14.
 */
public class OracleConverterTest {

    @Test
    public void testUUIDConverter() throws ConverterException {
        UUID uuid = UUID.randomUUID();
        OracleQuirks orclQuirks = new OracleQuirks();
        Converter<UUID> uuidConverter = new OracleUUIDConverter();

        byte[] rawUuid = (byte[])uuidConverter.toDatabaseParam(uuid);

        UUID reconvertedUuid = uuidConverter.convert(rawUuid);

        assertEquals(uuid, reconvertedUuid);

        // convert bytes to hex and put hyphens into the string to recreate the UUID string representation, just to be
        // sure everything is done correct.
        String hex = new HexBinaryAdapter().marshal(rawUuid);
        String hexUuid = String.format("%s-%s-%s-%s-%s",
                hex.substring(0,8),
                hex.substring(8,12),
                hex.substring(12, 16),
                hex.substring(16, 20),
                hex.substring(20)).toLowerCase();


        assertEquals(uuid.toString(), hexUuid);
    }

}
