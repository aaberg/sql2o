package org.sql2o;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.pojos.Post;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Connection;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author aldenquimby@gmail.com
 */
public class PerformanceTests
{
    private final static String DB_URL = "";
    private final static String DB_USER = "";
    private final static String DB_PASSWORD = "";

    private Sql2o sql2o;

    @Before
    public void setup()
    {
        sql2o = new Sql2o(new GenericDatasource(DB_URL, DB_USER, DB_PASSWORD));

        createAndFillPostTable();
    }

    private void createAndFillPostTable()
    {
        sql2o.createQuery("\n CREATE TABLE IF NOT EXISTS post" +
                          "\n (" +
                          "\n     id INT NOT NULL IDENTITY PRIMARY KEY" +
                          "\n   , text VARCHAR(255)" +
                          "\n   , creation_date VARCHAR(255)" +
                          "\n   , last_change_date VARCHAR(255)" +
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

        final int ROW_COUNT = 5000;
        Random r = new Random();

        Query insQuery = sql2o.createQuery("insert into post (text, creation_date, last_change_date, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9) values (:text, :creation_date, :last_change_date, :counter1, :counter2, :counter3, :counter4, :counter5, :counter6, :counter7, :counter8, :counter9)");
        for (int idx = 0; idx < ROW_COUNT; idx++)
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

    private Connection getConnection() {
        return new org.sql2o.Connection(sql2o, true).getJdbcConnection();
    }

    @Test
    public void run()
    {
        final int ITERATIONS = 500;
        System.out.println("Running " + ITERATIONS + " iterations that load up a post entity");
        run(ITERATIONS);
    }

    public void run(int iterations)
    {
        Tests tests = new Tests();

        // SQL2O
        tests.add("sql2o", new Action<Integer>()
        {
            @Override
            void run(Integer input)
            {
                sql2o.createQuery("SELECT * FROM posts WHERE id = :id").addParameter("id", input).executeAndFetchFirst(Post.class);
            }
        });

        // HAND CODED
        tests.add("hand coded", new HandCoded());

        // run the tests
        tests.run(iterations);

        // print the results
        for (OneTest test : tests.getTestsSortedByTime())
        {
            System.out.println(test.getName() + " took " + test.getWatch().elapsed(TimeUnit.MILLISECONDS) + "ms");
        }
    }

    class HandCoded extends Action<Integer>
    {
        private Connection conn = null;
        private PreparedStatement stmt = null;

        // create the connection and statement BEFORE running
        public HandCoded()
        {
            try {
                conn = getConnection();
                stmt = conn.prepareStatement("SELECT * FROM posts WHERE id = ?");
            }
            catch(SQLException se) {
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
                throw new RuntimeException("error when executing query", se);
            }
        }

        private Integer getNullableInt(ResultSet rs, String colName) throws SQLException {
            Object obj = rs.getObject(colName);
            return obj == null ? null : (Integer)obj;
        }

        @Override
        void run(Integer input)
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
    }
}

/**
 * Basically a {@link java.lang.Runnable} with an input.
 */
abstract class Action<T> implements Function<T, Void>
{
    public Void apply(T input)
    {
        run(input);
        return null;
    }

    abstract void run(T input);
}

class OneTest
{
    private Action<Integer> iteration;
    private String name;
    private Stopwatch watch;

    OneTest(Action<Integer> iteration, String name)
    {
        this.iteration = iteration;
        this.name = name;
        this.watch = Stopwatch.createUnstarted();
    }

    public Action<Integer> getIteration()
    {
        return iteration;
    }

    public String getName()
    {
        return name;
    }

    public Stopwatch getWatch()
    {
        return watch;
    }
}

class Tests
{
    private List<OneTest> tests = new ArrayList<OneTest>();

    public void run(int iterations)
    {
        // warm up
        for (OneTest test : tests)
        {
            test.getIteration().run(iterations + 1);
        }

        final Random rand = new Random();

        for (int i = 1; i <= iterations; i++)
        {
            Iterable<OneTest> sortedByRandom = orderBy(new Function<OneTest, Comparable>()
            {
                public Comparable apply(OneTest input)
                {
                    return rand.nextInt();
                }
            });

            for (OneTest test : sortedByRandom)
            {
                test.getWatch().start();
                test.getIteration().run(i);
                test.getWatch().stop();
            }
        }
    }

    public Iterable<OneTest> getTestsSortedByTime()
    {
        return orderBy(new Function<OneTest, Comparable>()
        {
            public Comparable apply(OneTest input)
            {
                return input.getWatch().elapsed(TimeUnit.MILLISECONDS);
            }
        });
    }

    public void add(String name, Action<Integer> iteration)
    {
        tests.add(new OneTest(iteration, name));
    }

    // helper to sort tests by a selector function
    private Iterable<OneTest> orderBy(Function<OneTest, ? extends Comparable> selector)
    {
        return Ordering.natural().onResultOf(selector).sortedCopy(tests);
    }
}
