package org.sql2o.issues;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.converters.joda.DateTimeConverter;
import org.sql2o.quirks.NoQuirks;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by lars on 05.10.2014.
 */
public class H2Tests {

    @Test
    public void testIssue155() {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        Sql2o sql2o = new Sql2o(ds, new NoQuirks(new HashMap<Class, Converter>() {{
            put(DateTime.class, new DateTimeConverter(DateTimeZone.getDefault()));
        }}));

        assertThat(sql2o.getQuirks(), is(instanceOf(NoQuirks.class)));

        try (Connection connection = sql2o.open()) {
            int val = connection.createQuery("select 42").executeScalar(Integer.class);

            assertThat(val, is(equalTo(42)));
        }
    }
}
