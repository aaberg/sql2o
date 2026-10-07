package org.sql2o.converters;

import java.util.UUID;

/**
 * {@link Db2UUIDConverter} against the contract of a uuid kept as sixteen bytes, which is what this database has instead of a
 * uuid type. The layout is the shared part; what differs is nothing, and that is the point of running the same test
 * against all three.
 */
public class Db2UUIDConverterTest extends BinaryUuidConverterTest {

    @Override
    protected Converter<UUID> converter() {
        return new Db2UUIDConverter();
    }
}