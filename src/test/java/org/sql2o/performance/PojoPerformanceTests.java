package org.sql2o.performance;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.service.ServiceRegistry;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.tools.FeatureDetector;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author aldenquimby@gmail.com
 */
public class PojoPerformanceTests
{
    private final static String DRIVER_CLASS = "org.h2.Driver";
    private final static String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private final static String DB_USER = "sa";
    private final static String DB_PASSWORD = "";
    private final static String HIBERNATE_DIALECT = "org.hibernate.dialect.H2Dialect";
    private final int ITERATIONS = 1000;

    @Before
    public void setup()
    {
        Logger.getLogger("org.hibernate").setLevel(Level.OFF);

        createPostTable();

        // turn off oracle because ResultSetUtils slows down with oracle
        setOracleAvailable(false);
    }

    private void createPostTable() {
        Sql2o sql2o = new Sql2o(DB_URL, DB_USER, DB_PASSWORD);

        sql2o.createQuery("DROP TABLE IF EXISTS post").executeUpdate();

        sql2o.createQuery("\n CREATE TABLE post" +
                "\n (" +
                "\n     id INT NOT NULL IDENTITY PRIMARY KEY" +
                "\n   , text VARCHAR(255)" +
                "\n   , creation_date DATETIME" +
                "\n   , last_change_date DATETIME" +
                "\n   , counter1 INT" +
                "\n   , counter2 INT" +
                "\n   , counter3 INT" +
                "\n   , counter4 INT" +
                "\n   , counter5 INT" +
                "\n   , counter6 INT" +
                "\n   , counter7 INT" +
                "\n   , counter8 INT" +
                "\n   , counter9 INT" +
                "\n )" +
                "\n;").executeUpdate();

        Random r = new Random();

        Query insQuery = sql2o.createQuery("insert into post (text, creation_date, last_change_date, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9) values (:text, :creation_date, :last_change_date, :counter1, :counter2, :counter3, :counter4, :counter5, :counter6, :counter7, :counter8, :counter9)");
        for (int idx = 0; idx < ITERATIONS; idx++)
        {
            insQuery.addParameter("text", "a name " + idx)
                    .addParameter("creation_date", new java.util.Date(idx * 5))
                    .addParameter("last_change_date", new java.util.Date(idx * 10))
                    .addParameter("counter1", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter2", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter3", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter4", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter5", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter6", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter7", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter8", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addParameter("counter9", r.nextDouble() > 0.5 ? r.nextInt() : null)
                    .addToBatch();
        }
        insQuery.executeBatch();
    }

    private void setOracleAvailable(boolean b) {
        try {
            Field f = FeatureDetector.class.getDeclaredField("oracleAvailable");
            f.setAccessible(true);
            f.set(null, b);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Test
    public void select()
    {
        System.out.println("Running " + ITERATIONS + " iterations that load up a Post entity\n");

        PerformanceTestList tests = new PerformanceTestList();
        tests.add(new HandCodedSelect());
        tests.add(new Sql2oOptimizedSelect());
        tests.add(new Sql2oTypicalSelect());
        tests.add(new HibernateTypicalSelect());
        tests.add(new JDBISelect());
        tests.add(new JOOQSelect());

        System.out.println("Warming up...");
        tests.run(ITERATIONS);
        System.out.println("Done warming up, let's rock and roll!\n");

        tests.run(ITERATIONS);
        tests.printResults();
    }

    //----------------------------------------
    //          performance tests
    // ---------------------------------------

    class Sql2oOptimizedSelect extends PerformanceTestBase
    {
        private org.sql2o.Connection conn;
        private Query query;

        @Override
        public void init()
        {
            conn = new Sql2o(DB_URL, DB_USER, DB_PASSWORD).open();
            query = conn.createQuery("SELECT text, creation_date as creationDate, last_change_date as lastChangeDate, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9 FROM post WHERE id = :id");
        }

        @Override
        public void run(int input)
        {
            query.addParameter("id", input)
                 .executeAndFetchFirst(Post.class);
        }

        @Override
        public void close()
        {
            conn.close();
        }
    }

    class Sql2oTypicalSelect extends PerformanceTestBase
    {
        private org.sql2o.Connection conn;
        private Query query;

        @Override
        public void init()
        {
            conn = new Sql2o(DB_URL, DB_USER, DB_PASSWORD).open();
            query = conn.createQuery("SELECT * FROM post WHERE id = :id")
                    .setAutoDeriveColumnNames(true);
        }

        @Override
        public void run(int input)
        {
            Post p = query.addParameter("id", input)
                 .executeAndFetchFirst(Post.class);
        }

        @Override
        public void close()
        {
            conn.close();
        }
    }

    class JDBISelect extends PerformanceTestBase{

        DBI dbi;
        Handle h;
        org.skife.jdbi.v2.Query<Post> q;

        @Override
        public void init() {
            dbi = new DBI(DB_URL, DB_USER, DB_PASSWORD);
            h = dbi.open();
            q = h.createQuery("SELECT * FROM post WHERE id = :id").map(Post.class);
        }

        @Override
        public void run(int input) {
            q.bind("id", input) .first();
        }

        @Override
        public void close() {
            h.close();
        }
    }

    class JOOQSelect extends PerformanceTestBase{

        DSLContext create;
        final String sql = "SELECT text, creation_date as creationDate, last_change_date as lastChangeDate, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9 FROM post WHERE id = ?";
        ResultQuery q;
        public void init() {

            try {
                create = DSL.using(DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD));
            } catch (SQLException e) {
                throw new RuntimeException("Error initializing jOOQ DSLContext", e);
            }

            q = create.select().from("post").where("id = ?", -1); // the parameter needs an initial value, else future calls the bind() will fail.
        }

        @Override
        public void run(int input) {
            Record rec = q.bind(1, input).fetchAny();

            // can't get the POJO parsing to work. p always just conatains empty fields.
            Post p = rec.into(Post.class);
        }

        @Override
        public void close() {
            q.close();
        }
    }

    class HandCodedSelect extends PerformanceTestBase
    {
        private Connection conn = null;
        private PreparedStatement stmt = null;

        @Override
        public void init()
        {
            try {
                conn = new Sql2o(DB_URL, DB_USER, DB_PASSWORD).open().getJdbcConnection();
                stmt = conn.prepareStatement("SELECT * FROM post WHERE id = ?");
            }
            catch(SQLException se) {
                throw new RuntimeException("error when executing query", se);
            }
        }

        private Integer getNullableInt(ResultSet rs, String colName) throws SQLException {
            Object obj = rs.getObject(colName);
            return obj == null ? null : (Integer)obj;
        }

        @Override
        public void run(int input)
        {
            ResultSet rs = null;

            try {
                stmt.setInt(1, input);

                rs = stmt.executeQuery();

                while(rs.next()) {
                    Post p = new Post();
                    p.setId(rs.getInt("id"));
                    p.setText(rs.getString("text"));
                    p.setCreationDate(rs.getDate("creation_date"));
                    p.setLastChangeDate(rs.getDate("last_change_date"));
                    p.setCounter1(getNullableInt(rs, "counter1"));
                    p.setCounter2(getNullableInt(rs, "counter2"));
                    p.setCounter3(getNullableInt(rs, "counter3"));
                    p.setCounter4(getNullableInt(rs, "counter4"));
                    p.setCounter5(getNullableInt(rs, "counter5"));
                    p.setCounter6(getNullableInt(rs, "counter6"));
                    p.setCounter7(getNullableInt(rs, "counter7"));
                    p.setCounter8(getNullableInt(rs, "counter8"));
                    p.setCounter9(getNullableInt(rs, "counter9"));
                }
            }
            catch (SQLException e) {
                throw new RuntimeException("error when executing query", e);
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close();
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void close()
        {
            if(stmt != null) {
                try {
                    stmt.close();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class HibernateTypicalSelect extends PerformanceTestBase
    {
        private Session session;

        @Override
        public void init()
        {
            Logger.getLogger("org.hibernate").setLevel(Level.OFF);

            Configuration cfg = new Configuration()
                    .setProperty("hibernate.connection.driver_class", DRIVER_CLASS)
                    .setProperty("hibernate.connection.url", DB_URL)
                    .setProperty("hibernate.connection.username", DB_USER)
                    .setProperty("hibernate.connection.password", DB_PASSWORD)
                    .setProperty("hibernate.dialect", HIBERNATE_DIALECT)
                    .setProperty("hbm2ddl.auto", "update")
                    .setNamingStrategy(ImprovedNamingStrategy.INSTANCE)
                    .addAnnotatedClass(Post.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(cfg.getProperties())
                    .build();

            SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry);
            session = sessionFactory.openSession();
        }

        @Override
        public void run(int input)
        {
            session.get(Post.class, input);
        }

        @Override
        public void close()
        {
            session.close();
        }
    }
}
