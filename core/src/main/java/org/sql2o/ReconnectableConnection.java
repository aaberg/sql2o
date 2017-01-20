package org.sql2o;

import org.sql2o.connectionsources.ConnectionSource;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;


class ReconnectableConnection extends BaseConnection {

    private final ConnectionSource connectionSource;


    ReconnectableConnection(Settings settings, ConnectionSource connectionSource, boolean autoClose) {
        super(settings, autoClose);
        this.connectionSource = requireNonNull(connectionSource, "connectionSource can't be null");
        createConnection();
    }

    @Override
    public Query createQuery(String queryText, boolean returnGeneratedKeys){

        try {
            if (jdbcConnection.isClosed()){
                createConnection();
            }
        } catch (SQLException e) {
            throw new Sql2oException("Error creating connection", e);
        }

        return new Query(this, queryText, returnGeneratedKeys);
    }

    @Override
    public Query createQuery(String queryText, String... columnNames) {
        try {
            if (jdbcConnection.isClosed()) {
                createConnection();
            }
        } catch(SQLException e) {
            throw new Sql2oException("Error creating connection", e);
        }

        return new Query(this, queryText, columnNames);
    }

    private void createConnection(){
        try{
            this.jdbcConnection = connectionSource.getConnection();
        }
        catch(Exception ex){
            throw new Sql2oException("Could not acquire a connection from DataSource - " + ex.getMessage(), ex);
        }
    }

}
