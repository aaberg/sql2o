package org.sql2o;

/**
 * Created by lars on 17.04.14.
 */
public interface AfterExecuteObservable {

    void attachAfterExecuteObserver(AfterExecuteObserver observer);

    void notifyAfterexecute();

}
