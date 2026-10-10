/*
 * Copyright (c) 2014 Lars Aaberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.quirks;

import org.sql2o.converters.Converter;
import org.sql2o.converters.HsqlUUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HSQLDB has no uuid type, so a uuid is kept as sixteen bytes of binary data and written through
 * {@link #setParameter(PreparedStatement, int, UUID)}. The typed {@code setParameter(Object)} routes a uuid there first,
 * which is what lets {@code addParameter("v", UUID.class, value)} work: the typed overload in core has no branch for a
 * uuid, so without that routing the value would reach the driver as the object itself. The untyped form needs no such
 * help, since it hands the value's own class to the same overload.
 *
 * <p>Column naming is left to {@link NoQuirks}, which returns the label of a column and therefore the alias a query gave
 * it, which is what HSQLDB does as well.
 *
 * <p>Two things HSQLDB does that are not this class's business, and are pinned by the tests that go with it instead: it
 * has no column type carrying a time zone at all, so a {@link java.time.OffsetDateTime} is stored without its offset and
 * gets the offset of the jvm back on the way out; and the driver shifts a {@link java.time.LocalTime} by the offset of
 * the jvm on the way in, so 12:34:56 arrives as 18:34:56 on a machine set to Europe/Moscow.
 */
public class HsqlQuirks extends NoQuirks {
    private static final HsqlUUIDConverter hsqlUUIDConverter = new HsqlUUIDConverter();

    public HsqlQuirks() {
        super(new HashMap<Class, Converter>() {{
            put(UUID.class, hsqlUUIDConverter);
        }});
    }

    public HsqlQuirks(Map<Class, Converter> converters) {
        super(converters);
    }

    /**
     * A uuid is routed to the overload that has one for it, so that naming the type reaches the sixteen bytes rather
     * than the object itself. {@code null} is not a uuid and falls through, and the branch before it is redundant:
     * {@code null instanceof UUID} is false.
     */
    @Override
    public void setParameter(PreparedStatement statement, int paramIdx, Object value) throws SQLException {
        if (value instanceof UUID) {
            setParameter(statement, paramIdx, (UUID) value);
        } else {
            statement.setObject(paramIdx, value);
        }
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx, UUID value) throws SQLException {
        statement.setBytes(paramIdx, (byte[]) hsqlUUIDConverter.toDatabaseParam(value));
    }
}