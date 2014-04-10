package org.sql2o.tools;

import junit.framework.TestCase;

/**
 * User: dimzon
 * Date: 4/9/14
 * Time: 4:44 AM
 */
public class NamedParameterTest extends TestCase {

    private NamedParameterHandlerFactory namedParameterHandlerFactory = new DefaultNamedParameterHandlerFactory();
    private NamedParameterHandler getNamedParameterHandler() {
        return namedParameterHandlerFactory.newParameterHandler();
    }

    /*
     A type cast specifies a conversion from one data type to another.
     PostgreSQL accepts two equivalent syntaxes for type casts:
     CAST ( expression AS type )
     expression::type
     */
    public void testPostgresSqlCastSyntax() throws Exception {
        String preparedQuery = getNamedParameterHandler().parseStatement("select :foo");
        assertEquals("select ?", preparedQuery);
        preparedQuery = getNamedParameterHandler().parseStatement("select (:foo)::uuid");
        assertEquals("select (?)::uuid", preparedQuery);
    }

    public void testStringConstant() throws Exception {
        String preparedQuery = getNamedParameterHandler().parseStatement("select ':foo'");
        assertEquals("select ':foo'", preparedQuery);
    }
}
