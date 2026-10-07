package org.sql2o.converters;

/**
 * HSQLDB has no uuid type, so a uuid is kept as the sixteen bytes of a {@code binary(16)} column, which is what
 * {@link BinaryUUIDConverter} does.
 *
 * <p>The class stays because it is part of this extension's published API, and because the column it is meant for is
 * HSQLDB's: {@code varbinary(16)} and {@code longvarbinary} hold the same bytes, but the second is a large object and
 * brings the trouble of a locator with it, so a uuid goes in a {@code binary} and the driver is asked for it through
 * {@code HsqlQuirks}, which registers this.
 *
 * @see org.sql2o.quirks.HsqlQuirks#setParameter(java.sql.PreparedStatement, int, java.util.UUID)
 */
public class HsqlUUIDConverter extends BinaryUUIDConverter {
}