package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 10/23/11
 * Time: 7:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sql2oException extends RuntimeException {

    public Sql2oException() {
    }

    public Sql2oException(String message) {
        super(message);
    }

    public Sql2oException(String message, Throwable cause) {
        super(message, cause);
    }

    public Sql2oException(Throwable cause) {
        super(cause);
    }
}
