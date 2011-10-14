package org.sql2o;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/21/11
 * Time: 10:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Entity {

    public long id;
    public String text;
    public Date time;
    public Timestamp ts;
    public Integer aNumber;
    public Long aLongNumber;


    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Timestamp getTs() {
        return ts;
    }

    public void setTs(Timestamp ts) {
        this.ts = ts;
    }

    public Integer getaNumber() {
        return aNumber;
    }

    public void setaNumber(Integer aNumber) {
        this.aNumber = aNumber;
    }
}
