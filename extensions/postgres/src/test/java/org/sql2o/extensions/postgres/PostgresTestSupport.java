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

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Sql2o;
import org.sql2o.converters.UUIDConverter;
import org.sql2o.quirks.PostgresQuirks;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by lars on 22.01.15.
 */
public class PostgresTestSupport {

    protected Sql2o sql2o;

    @Parameterized.Parameters(name = "{index} - {4}")
    public static Collection<Object[]> getData(){
        return Arrays.asList(new Object[][]{
                {"jdbc:postgresql:testdb", "test", "testtest", "Official postgres driver"},
//                {"jdbc:pgsql://localhost/testdb", "test", "testtest", "Impossibl postgres driver"}
        });
    }

    protected Logger logger = LoggerFactory.getLogger(PostgresTest.class);


    public PostgresTestSupport(String url, String user, String pass, String testName) {

        logger.info(testName);

        sql2o = new Sql2o(url, user, pass, new PostgresQuirks(){
            {
                // make sure we use default UUID converter.
                converters.put(UUID.class, new UUIDConverter());
            }
        });
    }

}
