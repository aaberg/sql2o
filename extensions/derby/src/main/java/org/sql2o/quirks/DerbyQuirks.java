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
import org.sql2o.converters.DerbyUUIDConverter;
import org.sql2o.converters.InstantToTimestampConverter;
import org.sql2o.converters.LocalDateTimeToTimestampConverter;
import org.sql2o.converters.LocalDateToSqlDateConverter;
import org.sql2o.converters.LocalTimeToSqlTimeConverter;
import org.sql2o.converters.OffsetDateTimeToTimestampConverter;
import org.sql2o.converters.OffsetTimeToTimeConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Derby takes a uuid as sixteen bytes of binary data, and its driver refuses every {@code java.time} type outright.
 *
 * <p>Three things are going on, all of them measured against Derby 10.14 rather than reasoned about.
 *
 * <p><b>A uuid.</b> Derby has no uuid type, so a uuid is written as the sixteen bytes a {@code varchar(n) for bit data}
 * column holds, through {@link #setParameter(PreparedStatement, int, UUID)}. The typed {@code setParameter(Object)}
 * routes a uuid there first, which is what lets {@code addParameter("v", UUID.class, value)} work: the typed overload in
 * core has no branch for a uuid, so without that routing the value would reach the driver as the object itself. The
 * untyped form needs no such help, since it hands the value's own class to the same overload.
 *
 * <p><b>The {@code java.time} types.</b> The driver predates them. {@code setObject} with a {@link LocalDate},
 * {@link LocalTime}, {@link LocalDateTime}, {@link Instant}, {@link OffsetDateTime} or {@link OffsetTime} fails with
 * {@code SQLDataException: An attempt was made to get a data value of type 'TIMESTAMP' from a data value of type
 * 'java.time.LocalDate'}, and asking the driver for an explicit target type does not help. The values are therefore
 * converted to the {@code java.sql} ones Derby does take, by the six converters of core that exist for exactly this
 * question: h2, hsqldb, postgres, oracle and db2 all take a {@code java.time} value as it is and register none of these.
 *
 * <p><b>Column naming.</b> Left to {@link NoQuirks}, which returns the label of a column and therefore the alias a query
 * gave it. Note that Derby folds an unquoted identifier to upper case, so {@code select val theVal} answers with the
 * label {@code THEVAL}; the mapping still works because name derivation is case insensitive, and
 * {@code DerbyColumnLabelTest} is where that is pinned.
 *
 * <p>Two things Derby does that are not this class's business, and are pinned by the tests that go with it instead: it
 * has no column type carrying a time zone at all, so an {@link OffsetDateTime} is stored without its offset and gets the
 * offset of the jvm back on the way out; and a {@code smallint} column answers {@code getObject} with an {@link Integer}
 * rather than a {@link Short}, which is a fact about the driver rather than about anything here.
 */
public class DerbyQuirks extends NoQuirks {
    private static final DerbyUUIDConverter derbyUUIDConverter = new DerbyUUIDConverter();

    public DerbyQuirks() {
        super(new HashMap<Class, Converter>() {{
            put(UUID.class, derbyUUIDConverter);
            put(Instant.class, new InstantToTimestampConverter());
            put(LocalDate.class, new LocalDateToSqlDateConverter());
            put(LocalTime.class, new LocalTimeToSqlTimeConverter());
            put(LocalDateTime.class, new LocalDateTimeToTimestampConverter());
            put(OffsetDateTime.class, new OffsetDateTimeToTimestampConverter());
            put(OffsetTime.class, new OffsetTimeToTimeConverter());
        }});
    }

    public DerbyQuirks(Map<Class, Converter> converters) {
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
        statement.setBytes(paramIdx, (byte[]) derbyUUIDConverter.toDatabaseParam(value));
    }
}