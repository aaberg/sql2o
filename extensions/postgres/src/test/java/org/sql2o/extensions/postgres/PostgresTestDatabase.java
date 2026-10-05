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

import org.sql2o.Sql2o;
import org.sql2o.converters.UUIDConverter;
import org.sql2o.quirks.PostgresQuirks;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * A postgres configuration a test runs against, together with the {@link Sql2o} built for it.
 *
 * Replaces the constructor injection of JUnit 4's {@code @RunWith(Parameterized.class)}, which JUnit 5 does not
 * offer. Same idea as core's {@code TestDatabase}, which is not on this module's classpath.
 */
public class PostgresTestDatabase {

    private final String name;
    private final String url;
    private final String user;
    private final String pass;
    private final Sql2o sql2o;

    private PostgresTestDatabase(String name, String url, String user, String pass) {
        this.name = name;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.sql2o = new Sql2o(url, user, pass, new PostgresQuirks() {
            {
                // make sure we use default UUID converter.
                converters.put(UUID.class, new UUIDConverter());
            }
        });
    }

    public static Stream<PostgresTestDatabase> databases() {
        return Stream.of(
                new PostgresTestDatabase("Official postgres driver",
                        "jdbc:postgresql://localhost:15432/postgres", "testuser", "testpassword")
        );
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public Sql2o getSql2o() {
        return sql2o;
    }

    @Override
    public String toString() {
        return name;
    }
}