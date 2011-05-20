package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class User {

    public Integer id;
    public String name;
    public String text;
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
