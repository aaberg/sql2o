package org.sql2o;

import java.util.stream.Stream;

/**
 * A database a single test method runs against, together with everything needed to talk to it.
 *
 * Replaces the constructor injection of JUnit 4's {@code @RunWith(Parameterized.class)}, which has no direct
 * JUnit 5 equivalent. The databases are provided by {@code TestDatabase#databases()} and reach the test method
 * through a single argument, see also {@link DatabaseTest}.
 */
public class TestDatabase {

    private final String name;
    private final String url;
    private final String user;
    private final String pass;
    private final Sql2o sql2o;

    public TestDatabase(String name, String url, String user, String pass) {
        this.name = name;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.sql2o = new Sql2o(url, user, pass);
    }

    public static Stream<TestDatabase> databases() {
        return Stream.of(
                new TestDatabase("H2 test", "jdbc:h2:mem:test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "")
        );
    }

    public String getName() {
        return name;
    }

    public Sql2o getSql2o() {
        return sql2o;
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

    @Override
    public String toString() {
        return name;
    }
}