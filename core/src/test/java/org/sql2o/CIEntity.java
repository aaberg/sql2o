package org.sql2o;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 8/28/11
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class CIEntity {
    public int id2;
    public String value2;
    public String someText;

    private String valWithGetter;

    public String getValWithGetter() {
        return valWithGetter;
    }

    public void setValWithGetter(String valWithGetter) {
        this.valWithGetter = valWithGetter;
    }
}
