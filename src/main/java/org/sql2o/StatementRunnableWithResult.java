package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 12/12/11
 * Time: 10:11 PM
 * To change this template use File | Settings | File Templates.
 */
public interface StatementRunnableWithResult {

    Object run(Connection connection, Object argument) throws Throwable;
}
