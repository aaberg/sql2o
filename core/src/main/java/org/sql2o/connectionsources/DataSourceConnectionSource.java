package org.sql2o.connectionsources;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The default implementation of {@link ConnectionSource},
 * Simply delegates all calls to specified {@link DataSource }
 * Created by nickl on 09.01.17.
 */
public class DataSourceConnectionSource implements ConnectionSource {

    private final DataSource dataSource;

    /**
     * Creates a ConnectionSource that gets connection from specified {@link DataSource }
     * @param dataSource a DataSource to get connections from
     */
    public DataSourceConnectionSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
