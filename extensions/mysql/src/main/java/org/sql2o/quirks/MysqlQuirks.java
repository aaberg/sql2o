/*
 * Copyright (c) 2026 Dmitry Alexandrov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.quirks;

import org.sql2o.converters.Converter;
import org.sql2o.converters.MysqlUUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL and MariaDB have no uuid type, so a uuid is kept as sixteen bytes of binary data and written through
 * {@link #setParameter(PreparedStatement, int, UUID)}. The typed {@code setParameter(Object)} routes a uuid there
 * first, which is what lets {@code addParameter("v", UUID.class, value)} work: the typed overload in core has no
 * branch for a uuid, so without that routing the value would reach the driver as the object itself.
 */
public class MysqlQuirks extends NoQuirks {
    private static final MysqlUUIDConverter mysqlUUIDConverter = new MysqlUUIDConverter();

    public MysqlQuirks() {
        super(new HashMap<Class, Converter>() {{
            put(UUID.class, mysqlUUIDConverter);
        }});
    }

    public MysqlQuirks(Map<Class, Converter> converters) {
        super(converters);
    }

    /**
     * A uuid is routed to the overload that has one for it, so that naming the type reaches the sixteen bytes rather
     * than the object itself. {@code null} is not a uuid and falls through.
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
        statement.setBytes(paramIdx, (byte[]) mysqlUUIDConverter.toDatabaseParam(value));
    }
}
