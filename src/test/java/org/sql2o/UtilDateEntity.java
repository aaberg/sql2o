package org.sql2o;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 8/30/11
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class UtilDateEntity {

    public int id;
    public Date d1;
    private Date d2;
    private Date d3;

    public Date getD2() {
        return d2;
    }

    public void setD2(Date d2) {
        this.d2 = d2;
    }

    public Date getD3() {
        return d3;
    }

    public void setD3(Date d3) {
        this.d3 = d3;
    }
}
