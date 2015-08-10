package org.sql2o.issues;

import org.h2.jdbcx.JdbcDataSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.converters.joda.DateTimeConverter;
import org.sql2o.data.Table;
import org.sql2o.quirks.H2Quirks;
import org.sql2o.quirks.NoQuirks;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by lars on 05.10.2014.
 */
public class H2Tests {

    DataSource ds;

    String driverClassName;
    String url;
    String user;
    String pass;

    @Before
    public void setUp() throws Exception {
        driverClassName = "org.h2.Driver";
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        user = "sa";
        pass = "";
        org.h2.jdbcx.JdbcDataSource datasource = new org.h2.jdbcx.JdbcDataSource();
        datasource.setURL(url);
        datasource.setUser(user);
        datasource.setPassword(pass);

        ds = datasource;
    }

    @Test
    public void testIssue155() {


        Sql2o sql2o = new Sql2o(ds, new NoQuirks(new HashMap<Class, Converter>() {{
            put(DateTime.class, new DateTimeConverter(DateTimeZone.getDefault()));
        }}));

        assertThat(sql2o.getQuirks(), is(instanceOf(NoQuirks.class)));

        try (Connection connection = sql2o.open()) {
            int val = connection.createQuery("select 42").executeScalar(Integer.class);

            assertThat(val, is(equalTo(42)));
        }
    }

    @Test
    public void testIssue172NPEWhenCreatingBasicDataSourceInline(){

        DataSource ds = new JdbcDataSource() {{
            setURL(url);
            setUser(user);
            setPassword(pass);
        }};

        Sql2o sql2o = new Sql2o(ds);

        assertThat(sql2o.getQuirks(), is(instanceOf(H2Quirks.class)));
    }

    /**
     * Ref issue #73
     */
    @Test
    public void testUUID()  {

        try (Connection connection = new Sql2o(ds).beginTransaction()) {
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
