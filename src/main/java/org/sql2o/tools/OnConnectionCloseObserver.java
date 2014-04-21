package org.sql2o.tools;

import java.sql.SQLException;

/**
 * Created by lars on 21.04.14.
 */
public interface OnConnectionCloseObserver {
    void update() throws SQLException;
}
