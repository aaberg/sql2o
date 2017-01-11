package org.sql2o.connectionsources;

import java.sql.SQLException;

/**
 * An abstraction layer for providing jdbc connection
 * to use from {@link org.sql2o.Connection}
 * Created by nickl on 09.01.17.
 */
public interface ConnectionSource {

    java.sql.Connection getConnection() throws SQLException;

}
