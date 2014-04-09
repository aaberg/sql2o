package org.sql2o.tools;

import junit.framework.TestCase;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: dimzon
 * Date: 4/9/14
 * Time: 4:44 AM
 */
public class NamedParameterStatementTests extends TestCase {
    private String realQuery;

    private NamedParameterStatement createStatement(String sql){
        try {
            return new NamedParameterStatement((Connection)
                    Proxy.newProxyInstance(this.getClass().getClassLoader(),
                            new Class[]{Connection.class}, new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            realQuery = (String) args[0];
                            return null;
                        }
                    }), sql, false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     A type cast specifies a conversion from one data type to another.
     PostgreSQL accepts two equivalent syntaxes for type casts:
     CAST ( expression AS type )
     expression::type
     */
    public void testPostgresSqlCastSyntax() throws Exception {
        createStatement("select :foo");
        assertEquals("select ?", realQuery);
        createStatement("select (:foo)::uuid");
        assertEquals("select (?)::uuid", realQuery);
    }

    public void testStringConstant() throws Exception {
        createStatement("select ':foo'");
        assertEquals("select ':foo'", realQuery);
    }
}
