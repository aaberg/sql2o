package org.sql2o.issues;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.*;
import org.sql2o.quirks.PostgresQuirks;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by lars on 14.04.14.
 */
public class ProcedureJdbcTest extends TestCase {

    private final Sql2o sql2o;
    public ProcedureJdbcTest() {
        sql2o = new Sql2o("jdbc:postgresql:testdb", "test", "testtest", new PostgresQuirks());
    }

    @Before
    public void setUp() {
        String createProc =
                "CREATE OR REPLACE FUNCTION testfunc(IN arg1 integer, OUT arg2 integer, OUT arg3 integer)\n" +
                "  RETURNS record AS\n" +
                "'select $1 + 1, $1 + 2'\n" +
                "  LANGUAGE sql VOLATILE\n" +
                "  COST 100;";
        sql2o.createQuery(createProc).executeUpdate();
    }

    @Test
    public void testJdbcProcedureCall() throws SQLException {
        final Integer inputArg = 20;
        Connection connection = sql2o.getDataSource().getConnection();
        CallableStatement callableStatement = null;

        Integer arg2;
        Integer arg3;

        try {
            callableStatement = connection.prepareCall("{call testfunc (?,?,?)}");

            callableStatement.setInt(1, inputArg);
            callableStatement.registerOutParameter(2, Types.INTEGER);
            callableStatement.registerOutParameter(3, Types.INTEGER);

            callableStatement.execute();
            arg2 = callableStatement.getInt(2);
            arg3 = callableStatement.getInt(3);
        } finally {
            if (!callableStatement.isClosed()){
                callableStatement.close();
            }
        }
        assertThat(arg2, is(equalTo(inputArg+1)));
        assertThat(arg3, is(equalTo(inputArg+2)));
    }

    @Test
    public void testSql2oProcedureCall() {
        final Integer inputArg = 20;
        Integer arg2, arg3;

        org.sql2o.Connection con = sql2o.open();

        OutParameter<Integer> p2 = new OutParameter<Integer>("outParam2", Types.INTEGER, Integer.class);
        OutParameter<Integer> p3 = new OutParameter<Integer>("outParam3", Types.INTEGER, Integer.class);

        con.createProcedureCall("{call testfunc (:inParam1,:outParam2,:outParam3)}")
                .addParameter("inParam1", inputArg)
                .addParameter(p2)
                .addParameter(p3)
                .executeUpdate();

        arg2 = p2.getValue();
        arg3 = p3.getValue();
        con.close();

        assertThat(arg2, is(equalTo(inputArg + 1)));
        assertThat(arg3, is(equalTo(inputArg+2)));

    }

    @Test
    public void testSql2oDirectProcedureCall() {
        final Integer inputArg = 20;
        Integer arg2, arg3;

        OutParameter<Integer> p2 = new OutParameter<Integer>("outParam2", Types.INTEGER, Integer.class);
        OutParameter<Integer> p3 = new OutParameter<Integer>("outParam3", Types.INTEGER, Integer.class);

        sql2o.createProcedureCall("{call testfunc (:inParam1,:outParam2,:outParam3)}")
                .addParameter("inParam1", inputArg)
                .addParameter(p2)
                .addParameter(p3)
                .executeUpdate();

        arg2 = p2.getValue();
        arg3 = p3.getValue();

        assertThat(arg2, is(equalTo(inputArg + 1)));
        assertThat(arg3, is(equalTo(inputArg+2)));

    }
}
