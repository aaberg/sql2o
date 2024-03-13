/*
 * Copyright (c) 2014 Lars Aaberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.issues;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Ignore;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.OracleQuirks;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: Lars Aaberg
 * Date: 20.02.13
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class OracleTest {

    private Sql2o sql2o;

    public OracleTest() {
        try {
            Class oracleDriverClass = this.getClass().getClassLoader().loadClass("oracle.jdbc.driver.OracleDriver");
            DriverManager.registerDriver((Driver)oracleDriverClass.newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        this.sql2o = new Sql2o("jdbc:oracle:thin:@//localhost:1521/XE", "system", "testpassword", new OracleQuirks());
    }

    /**
     * Issue #8
     * Cannot convert type class oracle.sql.TIMESTAMP to java.util.Date
     *
     *
     * Caused by: org.sql2o.converters.ConverterException: Cannot convert type class oracle.sql.TIMESTAMP to java.util.Date
     * at org.sql2o.converters.DateConverter.convert(DateConverter.java:25)
     * at org.sql2o.converters.DateConverter.convert(DateConverter.java:14)
     * at org.sql2o.reflection.Pojo.setProperty(Pojo.java:84)
     *
     *
     */
    @Test
    public void testForIssue8OracleTimestamps() {
        String sql = "select CURRENT_TIMESTAMP from dual";

        Date dateVal = sql2o.createQuery(sql).executeScalar(Date.class);
        DateTime dateTimeVal = sql2o.createQuery(sql).executeScalar(DateTime.class);

        assertThat(new DateTime(dateVal).toLocalDate(), is(equalTo(new LocalDate())));
        assertThat(dateTimeVal.toLocalDate(), is(equalTo(new LocalDate())));
    }

}
