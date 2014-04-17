package org.sql2o;

/**
 * Created by lars on 17.04.14.
 */
public interface AfterExecuteObserver {
    void update(Query query);
}
