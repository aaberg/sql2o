package org.sql2o;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.data.Table;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SQLiteTransactionalTest {

    private static final String DATABASE_JDBC_URL = "jdbc:sqlite::memory:";

    DataSource dataSource;

    private void initDataSource() {

        SQLiteConnectionPoolDataSource sqLiteDataSource = new SQLiteConnectionPoolDataSource();
        sqLiteDataSource.setUrl(DATABASE_JDBC_URL);
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(sqLiteDataSource);
        // Using single connection pool partly to keep in memory database alive
        hikariConfig.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(hikariConfig);
    }

    private void initDatabase() {

        java.sql.Connection connection = null;

        try {

            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();

            statement.execute("create table entity ("
                    + "id int identity primary key, "
                    + "text varchar(255))");

            connection.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void beforeTest() {

        initDataSource();
        initDatabase();
    }

    @After
    public void afterTest() {

        ((HikariDataSource) dataSource).close();
    }

    @Test
    public void test_default_transaction_isolation_level_error() {

        Sql2o sql2o = new Sql2o(dataSource);
        try {
            sql2o.beginTransaction();
            Assert.fail();
        } catch (Sql2oException e) {
            Assert.assertEquals("SQLite supports only TRANSACTION_SERIALIZABLE and TRANSACTION_READ_UNCOMMITTED.",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void test_with_appropriate_default_transaction_isolation_level() {
        
        int defaultTransactionIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE;

        Sql2o sql2o = new Sql2o(dataSource);
        sql2o.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
        
        Table table;

        try (Connection connection = sql2o.beginTransaction()) {
            executeInsert(connection);
            // no commit
        }
        
        table = executeQuery(sql2o);
        
        Assert.assertEquals(0, table.rows().size());

        try (Connection connection = sql2o.beginTransaction()) {
            executeInsert(connection);
            connection.commit();
        }
        
        table = executeQuery(sql2o);
        
        Assert.assertEquals(1, table.rows().size());
        Assert.assertEquals(table.rows().get(0).getObject("id"), 1);
        Assert.assertEquals(table.rows().get(0).getObject("text"), "FooBarTok");
        
        Assert.assertEquals(defaultTransactionIsolationLevel, sql2o.getDefaultTransactionIsolationLevel());
    }

    private Table executeQuery(Sql2o sql2o) {

        Table table;
        try (Connection connection = sql2o.open()) {
            table = connection.createQuery("select * from entity").executeAndFetchTable();
        }
        return table;
    }

    private void executeInsert(Connection connection) {

        connection.createQuery("insert into entity (id, text) values (:id, :text)")
            .addParameter("id", 1)
            .addParameter("text", "FooBarTok")
            .executeUpdate();
    }
}
