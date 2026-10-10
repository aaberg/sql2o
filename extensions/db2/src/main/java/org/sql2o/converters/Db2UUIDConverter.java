package org.sql2o.converters;

/**
 * DB2 has no uuid type, so a uuid is kept as the sixteen bytes of a {@code VARCHAR(16) FOR BIT DATA} column, which is
 * what {@link BinaryUUIDConverter} does.
 *
 * <p>The class stays because it is part of this extension's published API, and because the column it is meant for is
 * DB2's: {@code CHAR(16) FOR BIT DATA} and {@code BINARY(16)} hold the same bytes, but the first pads with blanks and the
 * second only exists from DB2 11.5 on, so a uuid goes in a {@code VARCHAR} and the driver is asked for it through
 * {@code Db2Quirks}, which registers this.
 *
 * @see org.sql2o.quirks.Db2Quirks#setParameter(java.sql.PreparedStatement, int, java.util.UUID)
 */
public class Db2UUIDConverter extends BinaryUUIDConverter {
}