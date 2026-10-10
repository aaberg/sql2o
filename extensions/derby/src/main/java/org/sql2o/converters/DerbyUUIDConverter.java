package org.sql2o.converters;

import java.util.UUID;

/**
 * Derby has no uuid type, so a uuid is kept as the sixteen bytes of a {@code varchar(16) for bit data} column, which is
 * what {@link BinaryUUIDConverter} does.
 *
 * <p>The class stays because it is part of this extension's published API, and because the column it is meant for is
 * Derby's: a uuid is always exactly sixteen bytes, so a {@code varchar} rather than a {@code char} is the one to use,
 * since a {@code char} pads what it holds with zeros to the length of the column and the sixteen bytes of a uuid would
 * then be read back with a tail that is not part of it.
 *
 * @see org.sql2o.quirks.DerbyQuirks#setParameter(java.sql.PreparedStatement, int, java.util.UUID)
 */
public class DerbyUUIDConverter extends BinaryUUIDConverter {
}