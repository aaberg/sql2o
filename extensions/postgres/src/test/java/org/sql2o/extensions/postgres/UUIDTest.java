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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.data.Table;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by lars on 22.01.15.
 */
@RunWith(Parameterized.class)
public class UUIDTest extends PostgresTestSupport {
    public UUIDTest(String url, String user, String pass, String testName) {
        super(url, user, pass, testName);
        logger.info("starting UUIDTest");
    }


    @Test
    public void testUUID() throws Exception {

        try (Connection connection = sql2o.beginTransaction()) {
            connection.createQuery("create table uuidtest(id uuid primary key, val uuid null)").executeUpdate();

            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();
            UUID uuid4 = null;

            Query insQuery = connection.createQuery("insert into uuidtest(id, val) values (:id, :val)");
            insQuery.addParameter("id", uuid1).addParameter("val", uuid2).executeUpdate();
            insQuery.addParameter("id", uuid3).addParameter("val", uuid4).executeUpdate();

            Table table = connection.createQuery("select * from uuidtest").executeAndFetchTable();

            assertThat((UUID)table.rows().get(0).getObject("id"), is(equalTo(uuid1)));
            assertThat((UUID)table.rows().get(0).getObject("val"), is(equalTo(uuid2)));
            assertThat((UUID)table.rows().get(1).getObject("id"), is(equalTo(uuid3)));
            assertThat(table.rows().get(1).getObject("val"), is(nullValue()));

            connection.rollback();
        }

    }
}
