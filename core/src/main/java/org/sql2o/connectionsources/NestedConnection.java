package org.sql2o.connectionsources;

import org.sql2o.logging.LocalLoggerFactory;
import org.sql2o.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by nickl on 09.01.17.
 */
class NestedConnection extends WrappedConnection {

    private final static Logger logger = LocalLoggerFactory.getLogger(NestedConnection.class);

    private boolean autocommit = true;

    NestedConnection(Connection source) {
        super(source);
    }


    private boolean commited = false;

    @Override
    public void commit() throws SQLException {
        commited = true;
        //do nothing, parent connection should be committed
    }

    @Override
    public void rollback() throws SQLException {
        if(!commited) {
            logger.warn("rollback of nested transaction leads to rollback of parent transaction. Maybe it is not wat you want.");
            super.rollback(); //probably it's worth to use savepoints
        }
    }

    @Override
    public void close() throws SQLException {
        //do nothing, parent connection should be closed by someone who cares
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        //do nothing, parent connection should be configured
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return autocommit;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autocommit = autoCommit;
    }



}
