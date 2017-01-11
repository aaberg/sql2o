package org.sql2o.connectionsources;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Predefined implementations of {@link ConnectionSource}
 * Created by nickl on 09.01.17.
 */
public class ConnectionSources {

    private ConnectionSources() {
    }

    /**
     * A ConnectionSource that will wrap externally managed connection
     * with proxy that will omit {@link Connection#close()} or {@link Connection#commit()} calls.
     * This is useful to make {@link org.sql2o.Connection} work with externally managed transactions
     * @param connection connection to wrap
     * @return a connection wrapper that represent a nested connection
     */
    public static ConnectionSource join(final Connection connection) {
        return new ConnectionSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return new NestedConnection(connection);
            }
        };
    }

}
