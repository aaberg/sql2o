/*
 * Copyright (c) 2014 Sql2o
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o;

import org.hsqldb.jdbcDriver;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by lars on 01.11.14.
 */

public class BaseMemDbTest {

    public enum DbType{
        H2("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""),
        HyperSQL("jdbc:hsqldb:mem:testmemdb", "SA", "");

        public final String url;
        public final String user;
        public final String pass;

        DbType(String url, String user, String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }
    }

    @Parameterized.Parameters(name = "{index} - {2}")
    public static Collection<Object[]> getData(){
        return Arrays.asList(new Object[][]{
                {DbType.H2, "H2 test"},
                {DbType.HyperSQL, "HyperSQL Test"}
        });
    }

    protected final DbType dbType;
    protected final Sql2o sql2o;

    public BaseMemDbTest(DbType dbType, String testName) {
        this.dbType = dbType;
        this.sql2o = new Sql2o(dbType.url, dbType.user, dbType.pass);

        if (dbType == DbType.HyperSQL) {
            try (Connection con = sql2o.open()){
                con.createQuery("set database sql syntax MSS true").executeUpdate();
            }
        }
    }
}
