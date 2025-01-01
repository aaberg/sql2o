package org.sql2o.extensions.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sql2o.Sql2o;

public class DataSourceTest {

    private Sql2o sql2o;

    @BeforeEach
    void setUp() {
        var dsConfig = new HikariConfig();
        dsConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        dsConfig.addDataSourceProperty("serverName", "localhost");
        dsConfig.addDataSourceProperty("portNumber", 15432);
        dsConfig.addDataSourceProperty("databaseName", "postgres");
        dsConfig.addDataSourceProperty("user", "testuser");
        dsConfig.addDataSourceProperty("password", "testpassword");
        dsConfig.setMaximumPoolSize(5);
        dsConfig.setSchema("public");

        // this is important to test, as it make the data source automatically open a connection when a connection is
        // created. This might break some functionality of sql2o.
        dsConfig.setAutoCommit(false);
        dsConfig.setReadOnly(false);

        var ds = new HikariDataSource(dsConfig);
        sql2o = new Sql2o(ds);
    }

    @Test
    void testQueryUsingHikariDataSource() {
        try (var con = sql2o.beginTransaction()) {
            var result = con.createQuery("SELECT 1").executeScalar(Integer.class);
            System.out.println(result);
        }
    }
}
