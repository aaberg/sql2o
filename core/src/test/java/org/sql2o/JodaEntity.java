package org.sql2o;

import org.joda.time.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 8/30/11
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class JodaEntity {

    public int id;

    public DateTime joda1;

    private DateTime joda2;

    public DateTime getJoda2() {
        return joda2;
    }

    public void setJoda2(DateTime joda2) {
        this.joda2 = joda2;
    }
}
