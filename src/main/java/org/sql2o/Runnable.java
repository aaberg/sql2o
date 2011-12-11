package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 12/11/11
 * Time: 12:00 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Runnable {

    void run(Connection connection) throws Throwable;
}
