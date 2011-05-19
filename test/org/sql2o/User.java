package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class User {

    public Long id;
    public String name;
    private String email;

    public Customer customer1;

    private Customer customer2;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Customer getCustomer2() {
        return customer2;
    }

    public void setCustomer2(Customer customer2) {
        this.customer2 = customer2;
    }
}
