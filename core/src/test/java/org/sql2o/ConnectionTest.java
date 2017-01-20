package org.sql2o;

import junit.framework.TestCase;
import org.sql2o.quirks.NoQuirks;

import javax.sql.DataSource;

import java.sql.*;
import java.sql.Connection;

import static org.mockito.Mockito.*;

/**
 * User: dimzon
 * Date: 4/29/14
 * Time: 10:05 PM
 */
public class ConnectionTest extends TestCase {



    public void test_createQueryWithParams() throws Throwable {
        DataSource dataSource = mock(DataSource.class);
        Connection jdbcConnection = mock(Connection.class);
        when(jdbcConnection.isClosed()).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(jdbcConnection);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(jdbcConnection.prepareStatement(anyString())).thenReturn(ps);

        Sql2o sql2o = new Sql2o(dataSource,new NoQuirks(){
            @Override
            public boolean returnGeneratedKeysByDefault() {
                return false;
            }
        });
        org.sql2o.Connection cn = new ReconnectableConnection(sql2o.getSettings(), sql2o.getConnectionSource(),false);
        cn.createQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildPreparedStatement();

        verify(dataSource,times(1)).getConnection();
        verify(jdbcConnection).isClosed();
        verify(jdbcConnection,times(1)).prepareStatement("select ? name, ? age");
        verify(ps,times(1)).setString(1,"Dmitry Alexandrov");
        verify(ps,times(1)).setInt(2,35);
        // check statement still alive
        verify(ps,never()).close();

    }

    public class MyException extends RuntimeException{}

    public void test_createQueryWithParamsThrowingException() throws Throwable {
        DataSource dataSource = mock(DataSource.class);
        Connection jdbcConnection = mock(Connection.class);
        when(jdbcConnection.isClosed()).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(jdbcConnection);
        PreparedStatement ps = mock(PreparedStatement.class);
        doThrow(MyException.class).when(ps).setInt(anyInt(),anyInt());
        when(jdbcConnection.prepareStatement(anyString())).thenReturn(ps);

        Sql2o sql2o = new Sql2o(dataSource,new NoQuirks(){
            @Override
            public boolean returnGeneratedKeysByDefault() {
                return false;
            }
        });
        try(org.sql2o.Connection cn = sql2o.open()){
            cn.createQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildPreparedStatement();
            fail("exception not thrown");
        } catch (MyException ex){
            // as designed
        }
        verify(dataSource,times(1)).getConnection();
        verify(jdbcConnection,atLeastOnce()).isClosed();
        verify(jdbcConnection,times(1)).prepareStatement("select ? name, ? age");
        verify(ps,times(1)).setInt(2,35);
        // check statement was closed
        verify(ps,times(1)).close();
    }
}
