package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/19/11
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class Customer {

    public Long id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
