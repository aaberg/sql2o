package org.sql2o;

import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by lars on 25.01.15.
 */
public class QueryTest {

    /**
     * Test issue 161, fetch trigger generated keys from Oracle.
     * Fixed by calling java.sql.Connection.prepareStatement(String, String[]) overload.
     * @throws SQLException
     */
    @Test
    public void testCreateQueryWithKeyNames() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        java.sql.Connection jdbcCon = mock(java.sql.Connection.class);

        when(jdbcCon.isClosed()).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(jdbcCon);

        Sql2o sql2o = new Sql2o(dataSource);

        Connection con = sql2o.open();
        final Query query = con.createQuery("sql", "colname");

        verify(jdbcCon, times(1)).prepareStatement(anyString(), new String[]{anyString()});

    }

}
