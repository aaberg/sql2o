package org.sql2o.extensions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;
import org.sql2o.data.Table;
import org.sql2o.quirks.MySQLQuirks;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class MySQLTest {
    private final Sql2o sql2o;
    private final Logger logger = LoggerFactory.getLogger(MySQLTest.class);

    public MySQLTest(String url, String user, String pass, String testName) {
        logger.info(testName);
        sql2o = new Sql2o(url, user, pass, new MySQLQuirks());
        logger.info("starting MySQLTest");
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> getData(){
        return Arrays.asList(new Object[][]{
            {"jdbc:tc:mysql:5.7.34:///testdb", "test", "testtest", "Official MySQL driver"},
        });
    }

    @Test
    public void testMySQL_whenJDBCReturnLocalDateTime() {
        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table test_table(created_datetime datetime(6))").executeUpdate();
            //MySQL date(6) stores value to microseconds (6 digits) precision
            Timestamp now = Timestamp.valueOf("2021-10-20 21:29:42.345678");
            connection.createQuery("insert into test_table (created_datetime) values(:created_datetime)")
                .addParameter("created_datetime", now).executeUpdate();

            String selectSql = "select created_datetime from test_table";
            Table resultTable = connection.createQuery(selectSql).executeAndFetchTable();

            assertThat(resultTable.rows().size(), is(1));
            Row resultRow = resultTable.rows().get(0);

            assertThat(resultRow.getObject("created_datetime", Timestamp.class), equalTo(now));
        }
    }
}
