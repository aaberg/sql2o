package org.sql2o.pojos;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 12/6/11
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComplexEntity {
    
    public int id;

    private EntityWithPrivateFields entity;

    public EntityWithPrivateFields getEntity() {
       return entity;
    }

    public void setEntity(EntityWithPrivateFields entity) {
        this.entity = entity;
    }

}