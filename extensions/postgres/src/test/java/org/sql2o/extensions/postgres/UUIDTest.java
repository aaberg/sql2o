/*
 * Copyright (c) 2015 Sql2o
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.extensions.postgres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.data.Table;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by lars on 22.01.15.
 */
public class UUIDTest extends PostgresTestSupport {

    static Stream<PostgresTestDatabase> databases() {
        return PostgresTestDatabase.databases();
    }

    @BeforeEach
    public void announceTheTestClass() {
        logger.info("starting UUIDTest");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    public void testUUID(PostgresTestDatabase db) throws Exception {

        try (Connection connection = db.getSql2o().beginTransaction()) {
            connection.createQuery("create table uuidtest(id uuid primary key, val uuid null)").executeUpdate();

            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();
            UUID uuid4 = null;

            Query insQuery = connection.createQuery("insert into uuidtest(id, val) values (:id, :val)");
            insQuery.addParameter("id", uuid1).addParameter("val", uuid2).executeUpdate();
            insQuery.addParameter("id", uuid3).addParameter("val", uuid4).executeUpdate();

            Table table = connection.createQuery("select * from uuidtest").executeAndFetchTable();

            assertThat((UUID) table.rows().get(0).getObject("id"), is(equalTo(uuid1)));
            assertThat((UUID) table.rows().get(0).getObject("val"), is(equalTo(uuid2)));
            assertThat((UUID) table.rows().get(1).getObject("id"), is(equalTo(uuid3)));
            assertThat(table.rows().get(1).getObject("val"), is(nullValue()));

            connection.rollback();
        }

    }
}