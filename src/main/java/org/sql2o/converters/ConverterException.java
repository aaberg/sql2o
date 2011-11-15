package org.sql2o.converters;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/14/11
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConverterException extends Exception{

    public ConverterException(String message) {
        super(message);
    }

    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
