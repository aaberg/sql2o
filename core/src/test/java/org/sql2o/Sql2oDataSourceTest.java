package org.sql2o;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 10/5/12
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sql2oDataSourceTest extends TestCase {

    private Sql2o sql2o;

    private String url = "jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1";
    private String user = "sa";
    private String pass = "";

    @Override
    protected void setUp() throws Exception {
        DataSource ds = JdbcConnectionPool.create(url, user, pass);

        sql2o = new Sql2o(ds);
    }

    public void testExecuteAndFetchWithNulls(){
        String sql =
                "create table testExecWithNullsTbl (" +
                        "id int identity primary key, " +
                        "text varchar(255), " +
                        "aNumber int, " +
                        "aLongNumber bigint)";
        sql2o.createQuery(sql, "testExecuteAndFetchWithNulls").executeUpdate();

        sql2o.runInTransaction(new StatementRunnable() {
            public void run(Connection connection, Object argument) throws Throwable {
                Query insQuery = connection.createQuery("insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
                insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
                insQuery.addParameter("text", "some text").addParameter("number", (Integer)null).addParameter("lnum", 10L).executeUpdate();
                insQuery.addParameter("text", (String)null).addParameter("number", 21).addParameter("lnum", (Long)null).executeUpdate();
                insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
                insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
            }
        });

        List<Entity> fetched = sql2o.createQuery("select * from testExecWithNullsTbl").executeAndFetch(Entity.class);

        assertTrue(fetched.size() == 5);
        assertNull(fetched.get(2).text);
        assertNotNull(fetched.get(3).text);

        assertNull(fetched.get(1).aNumber);
        assertNotNull(fetched.get(2).aNumber);

        assertNull(fetched.get(2).aLongNumber);
        assertNotNull(fetched.get(3).aLongNumber);


    }
}
