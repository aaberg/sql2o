package org.sql2o.pojos;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/19/11
 * Time: 5:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntityWithPrivateFields {
    
    private int id;
    
    private String value;
    
    public int getId(){
        return id;
    }
    
    private void setValue(String value){
        this.value = value + "1";
    }
    
    public String getValue(){
        return value;
    }
}
