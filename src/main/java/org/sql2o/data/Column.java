package org.sql2o.data;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/19/11
 * Time: 8:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Column {
    
    private String name;
    private Integer index;
    private String type;


    public Column(String name, Integer index, String type) {
        this.name = name;
        this.index = index;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Integer getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getName() + " (" + getType() + ")";
    }
}
