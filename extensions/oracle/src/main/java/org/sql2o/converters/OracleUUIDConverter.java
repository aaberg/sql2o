package org.sql2o.converters;

/**
 * Oracle has no uuid type, so a uuid is kept as the sixteen bytes of a {@code RAW(16)} column, which is what
 * {@link BinaryUUIDConverter} does.
 *
 * <p>The class stays because it is part of this extension's published API, and because the column it is meant for is
 * Oracle's: a driver asked to write a uuid does so through {@code OracleQuirks}, which registers this.
 *
 * @see org.sql2o.quirks.OracleQuirks#setParameter(java.sql.PreparedStatement, int, java.util.UUID)
 */
public class OracleUUIDConverter extends BinaryUUIDConverter {
}