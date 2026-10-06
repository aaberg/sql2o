package org.sql2o.converters;

/**
 * MySQL and MariaDB have no uuid type, so a uuid is kept as the sixteen bytes of a {@code binary(16)} column, which
 * is what {@link BinaryUUIDConverter} does.
 *
 * <p>The class stays because it is part of this extension's published API, and because the column it is meant for is
 * this database's: a uuid goes in a {@code binary(16)}, and the driver is asked for it through {@code MysqlQuirks},
 * which registers this.
 *
 * @see org.sql2o.quirks.MysqlQuirks#setParameter(java.sql.PreparedStatement, int, java.util.UUID)
 */
public class MysqlUUIDConverter extends BinaryUUIDConverter {
}
